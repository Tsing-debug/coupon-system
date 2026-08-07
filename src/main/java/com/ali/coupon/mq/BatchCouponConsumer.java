package com.ali.coupon.mq;

import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import com.ali.coupon.coupon.service.UserCouponService;
import com.ali.coupon.service.BatchJobRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RocketMQMessageListener(topic = "coupon-batch-issue", consumerGroup = "batch-coupon-group")
public class BatchCouponConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(BatchCouponConsumer.class);

    private final UserCouponMapper userCouponMapper;
    private final UserCouponService userCouponService;
    private final BatchJobRecordService jobRecordService;
    private final CouponExpireProducer expireProducer;
    private final ObjectMapper objectMapper;

    public BatchCouponConsumer(UserCouponMapper userCouponMapper,
                               UserCouponService userCouponService,
                               BatchJobRecordService jobRecordService,
                               CouponExpireProducer expireProducer,
                               ObjectMapper objectMapper) {
        this.userCouponMapper = userCouponMapper;
        this.userCouponService = userCouponService;
        this.jobRecordService = jobRecordService;
        this.expireProducer = expireProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(String messageBody) {
        BatchCouponMessage msg;
        try {
            msg = objectMapper.readValue(messageBody, BatchCouponMessage.class);
        } catch (Exception e) {
            log.error("批量消息反序列化失败: body={}", messageBody, e);
            return;
        }

        log.info("收到批量发券消息: jobId={}, batch={}/{}, users={}",
                msg.getJobId(), msg.getBatchIndex(), msg.getTotalBatches(), msg.getUserIdList().size());

        int successCount = 0;
        int failCount = 0;

        for (Long userId : msg.getUserIdList()) {
            UserCoupon coupon = buildUserCoupon(userId, msg);
            try {
                userCouponMapper.insert(coupon);

                // 逐条写 ZSet 索引 + detail 缓存
                userCouponService.indexAfterIssue(coupon);

                // 发送延迟过期消息
                Date validEndDate = Date.from(coupon.getValidEndTime()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant());
                expireProducer.sendDelayExpireMsg(coupon.getId(), validEndDate);

                successCount++;
            } catch (DuplicateKeyException e) {
                failCount++;
                log.debug("幂等跳过: userId={}, batchNo={}", userId, coupon.getBatchNo());
            }
        }

        log.info("批量落库完成: jobId={}, batch={}, 成功={}, 重复/失败={}",
                msg.getJobId(), msg.getBatchIndex(), successCount, failCount);

        for (int i = 0; i < successCount; i++) {
            jobRecordService.incrementSuccess(msg.getJobId());
        }
        for (int i = 0; i < failCount; i++) {
            jobRecordService.incrementFail(msg.getJobId());
        }

        if (failCount > 0) {
            jobRecordService.markPartialFail(msg.getJobId());
        }

        clearSettlementCache(msg);
    }

    private UserCoupon buildUserCoupon(Long userId, BatchCouponMessage msg) {
        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(userId);
        coupon.setTemplateId(msg.getTemplateId());
        coupon.setShopNumber(msg.getShopNumber());
        coupon.setBatchNo(msg.getJobId() + ":" + msg.getBatchIndex() + ":" + userId);
        coupon.setCouponAmount(new BigDecimal(msg.getCouponAmount()));
        coupon.setStatus(1);
        coupon.setValidStartTime(LocalDateTime.now());
        coupon.setValidEndTime(LocalDateTime.now().plusDays(30));
        return coupon;
    }

    private void clearSettlementCache(BatchCouponMessage msg) {
        log.debug("结算缓存清除事件: jobId={}, users={}", msg.getJobId(), msg.getUserIdList().size());
    }
}
