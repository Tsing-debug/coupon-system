package com.ali.coupon.mq;

import com.ali.coupon.service.CouponExpireService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(topic = "coupon-expire", consumerGroup = "coupon-expire-group")
public class CouponExpireConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireConsumer.class);

    private final CouponExpireService expireService;

    public CouponExpireConsumer(CouponExpireService expireService) {
        this.expireService = expireService;
    }

    @Override
    public void onMessage(String couponIdStr) {
        Long couponId;
        try {
            couponId = Long.parseLong(couponIdStr);
        } catch (NumberFormatException e) {
            log.error("过期消息格式错误: {}", couponIdStr);
            return;
        }

        log.info("收到过期延迟消息: couponId={}", couponId);
        expireService.handleExpire(couponId);
    }
}
