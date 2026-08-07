package com.ali.coupon.mq;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class CouponExpireProducer {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireProducer.class);

    private static final String TOPIC = "coupon-expire";

    // RocketMQ 延迟级别: 1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m,
    // 9=5m, 10=6m, 11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
    private static final int DELAY_LEVEL_30M = 16; // 30分钟
    private static final int DELAY_LEVEL_TEST = 3; // 10s 测试用

    private final RocketMQTemplate rocketMQTemplate;

    public CouponExpireProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 发送延迟过期消息
     * @param couponId 用户券ID
     * @param validEnd 有效期截止时间
     * @param delayLevel RocketMQ延迟级别
     */
    public void sendDelayExpireMsg(Long couponId, Date validEnd, int delayLevel) {
        CouponExpireMessage body = new CouponExpireMessage();
        body.setCouponId(couponId);
        body.setValidEnd(validEnd);

        Message<String> message = MessageBuilder
                .withPayload(couponId.toString())
                .setHeader(RocketMQHeaders.KEYS, "expire:" + couponId)
                .setHeader("delayTimeLevel", delayLevel)
                .build();

        // 使用 syncSend 并指定延迟级别
        SendResult result = rocketMQTemplate.syncSend(TOPIC, message, 3000, delayLevel);
        log.info("延迟过期消息已发送: couponId={}, delayLevel={}, msgId={}",
                couponId, delayLevel, result.getMsgId());
    }

    public void sendDelayExpireMsg(Long couponId, Date validEnd) {
        sendDelayExpireMsg(couponId, validEnd, DELAY_LEVEL_30M);
    }

    /**
     * 消息体内嵌类
     */
    public static class CouponExpireMessage {
        private Long couponId;
        private Date validEnd;

        public Long getCouponId() { return couponId; }
        public void setCouponId(Long couponId) { this.couponId = couponId; }

        public Date getValidEnd() { return validEnd; }
        public void setValidEnd(Date validEnd) { this.validEnd = validEnd; }
    }
}
