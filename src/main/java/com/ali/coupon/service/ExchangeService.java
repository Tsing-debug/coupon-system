package com.ali.coupon.service;

import com.ali.coupon.dto.ExchangeResult;
import com.ali.coupon.mq.CouponMessage;
import com.ali.coupon.mq.RocketMQProducer;
import com.ali.coupon.outbox.entity.OutboxTask;
import com.ali.coupon.outbox.mapper.OutboxTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExchangeService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeService.class);

    private final LuaScriptService luaScriptService;
    private final OutboxTaskMapper outboxTaskMapper;
    private final RocketMQProducer rocketMQProducer;

    public ExchangeService(LuaScriptService luaScriptService,
                           OutboxTaskMapper outboxTaskMapper,
                           RocketMQProducer rocketMQProducer) {
        this.luaScriptService = luaScriptService;
        this.outboxTaskMapper = outboxTaskMapper;
        this.rocketMQProducer = rocketMQProducer;
    }

    /**
     * 热点券兑换主流程
     * 1. Lua 原子扣 Redis 库存 + 防重
     * 2. 落本地消息表 (@Transactional 仅管 MySQL)
     * 3. 发送 MQ (失败则等 XXL-Job 补偿)
     */
    @Transactional
    public ExchangeResult exchange(Long userId, Long activityId, Long templateId,
                                   String shopNumber, String couponAmount) {

        Long luaResult = luaScriptService.executeExchange(templateId, userId, activityId);
        if (luaResult != 1) {
            return switch (luaResult.intValue()) {
                case -1 -> ExchangeResult.stockInsufficient();
                case -2 -> ExchangeResult.duplicate();
                default -> ExchangeResult.fail(luaResult.intValue());
            };
        }

        String batchNo = userId + ":" + templateId + ":" + activityId;
        String businessKey = batchNo;

        OutboxTask task = new OutboxTask();
        task.setTaskType("COUPON_ISSUE");
        task.setBusinessKey(businessKey);
        task.setPayload(buildPayload(userId, templateId, activityId, batchNo, shopNumber, couponAmount));
        task.setStatus(0);
        task.setRetryCount(0);
        task.setMaxRetry(5);
        task.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        outboxTaskMapper.insert(task);

        try {
            CouponMessage msg = buildMessage(userId, templateId, activityId, batchNo, businessKey, shopNumber, couponAmount);
            rocketMQProducer.send(msg);
        } catch (Exception e) {
            log.error("MQ发送失败, 等待XXL-Job补偿, businessKey={}", businessKey, e);
        }

        return ExchangeResult.success();
    }

    private String buildPayload(Long userId, Long templateId, Long activityId,
                                String batchNo, String shopNumber, String couponAmount) {
        return String.format(
            "{\"userId\":%d,\"templateId\":%d,\"activityId\":%d,\"batchNo\":\"%s\",\"shopNumber\":\"%s\",\"couponAmount\":\"%s\"}",
            userId, templateId, activityId, batchNo, shopNumber, couponAmount);
    }

    private CouponMessage buildMessage(Long userId, Long templateId, Long activityId,
                                       String batchNo, String businessKey,
                                       String shopNumber, String couponAmount) {
        CouponMessage msg = new CouponMessage();
        msg.setUserId(userId);
        msg.setTemplateId(templateId);
        msg.setActivityId(activityId);
        msg.setBatchNo(batchNo);
        msg.setBusinessKey(businessKey);
        msg.setShopNumber(shopNumber);
        msg.setCouponAmount(couponAmount);
        return msg;
    }
}
