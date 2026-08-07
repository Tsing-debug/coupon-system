package com.ali.coupon.mq;

import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import com.ali.coupon.coupon.service.UserCouponService;
import com.ali.coupon.outbox.mapper.OutboxTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RocketMQMessageListener(topic = "coupon-exchange", consumerGroup = "coupon-group")
public class CreateCouponConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(CreateCouponConsumer.class);

    private final UserCouponMapper userCouponMapper;
    private final OutboxTaskMapper outboxTaskMapper;
    private final UserCouponService userCouponService;
    private final CouponExpireProducer expireProducer;
    private final ObjectMapper objectMapper;

    public CreateCouponConsumer(UserCouponMapper userCouponMapper,
                                OutboxTaskMapper outboxTaskMapper,
                                UserCouponService userCouponService,
                                CouponExpireProducer expireProducer,
                                ObjectMapper objectMapper) {
        this.userCouponMapper = userCouponMapper;
        this.outboxTaskMapper = outboxTaskMapper;
        this.userCouponService = userCouponService;
        this.expireProducer = expireProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String messageBody) {
        CouponMessage msg;
        try {
            msg = objectMapper.readValue(messageBody, CouponMessage.class);
        } catch (Exception e) {
            log.error("消息反序列化失败, body={}", messageBody, e);
            return;
        }

        log.info("收到兑换消息: businessKey={}, userId={}, templateId={}",
                msg.getBusinessKey(), msg.getUserId(), msg.getTemplateId());

        try {
            UserCoupon coupon = new UserCoupon();
            coupon.setUserId(msg.getUserId());
            coupon.setTemplateId(msg.getTemplateId());
            coupon.setShopNumber(msg.getShopNumber());
            coupon.setBatchNo(msg.getBatchNo());
            coupon.setCouponAmount(new BigDecimal(msg.getCouponAmount()));
            coupon.setStatus(1);
            coupon.setValidStartTime(LocalDateTime.now());
            coupon.setValidEndTime(LocalDateTime.now().plusDays(30));

            userCouponMapper.insert(coupon);

            outboxTaskMapper.updateStatusByBusinessKey(msg.getBusinessKey(), 1);

            // 写 ZSet 索引 + detail 缓存，结算页可查
            userCouponService.indexAfterIssue(coupon);

            // 发送延迟过期消息（30分钟后自动处理过期）
            Date validEndDate = Date.from(coupon.getValidEndTime()
                    .atZone(ZoneId.systemDefault()).toInstant());
            expireProducer.sendDelayExpireMsg(coupon.getId(), validEndDate);

            log.info("券落库成功: businessKey={}, couponId={}", msg.getBusinessKey(), coupon.getId());

        } catch (DuplicateKeyException e) {
            log.warn("幂等拦截: 重复消费, businessKey={}, 直接ACK", msg.getBusinessKey());
        }
    }
}
