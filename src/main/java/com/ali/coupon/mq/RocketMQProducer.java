package com.ali.coupon.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class RocketMQProducer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQProducer.class);

    private static final String TOPIC = "coupon-exchange";

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public RocketMQProducer(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
    }

    public SendResult send(CouponMessage msg) {
        Message<String> message = MessageBuilder
                .withPayload(toJson(msg))
                .setHeader(RocketMQHeaders.KEYS, msg.getBusinessKey())
                .build();

        SendResult result = rocketMQTemplate.syncSend(TOPIC, message);
        log.info("MQ sent: businessKey={}, msgId={}, status={}",
                msg.getBusinessKey(), result.getMsgId(), result.getSendStatus());
        return result;
    }

    private String toJson(CouponMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            throw new RuntimeException("序列化CouponMessage失败", e);
        }
    }
}
