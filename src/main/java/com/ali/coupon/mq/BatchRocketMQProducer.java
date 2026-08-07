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
public class BatchRocketMQProducer {

    private static final Logger log = LoggerFactory.getLogger(BatchRocketMQProducer.class);

    private static final String TOPIC = "coupon-batch-issue";

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public BatchRocketMQProducer(RocketMQTemplate rocketMQTemplate, ObjectMapper objectMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
    }

    public SendResult send(BatchCouponMessage msg) {
        Message<String> message = MessageBuilder
                .withPayload(toJson(msg))
                .setHeader(RocketMQHeaders.KEYS, msg.getJobId() + ":" + msg.getBatchIndex())
                .build();

        SendResult result = rocketMQTemplate.syncSend(TOPIC, message);
        log.info("批量MQ已发送: jobId={}, batch={}/{}, users={}, msgId={}",
                msg.getJobId(), msg.getBatchIndex(), msg.getTotalBatches(),
                msg.getUserIdList().size(), result.getMsgId());
        return result;
    }

    private String toJson(BatchCouponMessage msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            throw new RuntimeException("序列化BatchCouponMessage失败", e);
        }
    }
}
