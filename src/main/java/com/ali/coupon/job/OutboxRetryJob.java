package com.ali.coupon.job;

import com.ali.coupon.mq.CouponMessage;
import com.ali.coupon.mq.RocketMQProducer;
import com.ali.coupon.outbox.entity.OutboxTask;
import com.ali.coupon.outbox.mapper.OutboxTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxRetryJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRetryJob.class);

    private final OutboxTaskMapper outboxTaskMapper;
    private final RocketMQProducer rocketMQProducer;

    public OutboxRetryJob(OutboxTaskMapper outboxTaskMapper,
                          RocketMQProducer rocketMQProducer) {
        this.outboxTaskMapper = outboxTaskMapper;
        this.rocketMQProducer = rocketMQProducer;
    }

    /**
     * 每分钟扫描待发送的 outbox_task，重新投递 MQ
     * 指数退避: next_retry_time = now + 2^retryCount 分钟
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scanAndRetry() {
        log.debug("OutboxRetryJob 开始扫描...");

        List<OutboxTask> tasks = outboxTaskMapper.selectList(
                new LambdaQueryWrapper<OutboxTask>()
                        .eq(OutboxTask::getStatus, 0)
                        .lt(OutboxTask::getNextRetryTime, LocalDateTime.now())
                        .last("LIMIT 100")
        );

        if (tasks.isEmpty()) {
            log.debug("无待补偿任务");
            return;
        }

        log.info("发现 {} 条待补偿任务", tasks.size());

        for (OutboxTask task : tasks) {
            try {
                CouponMessage msg = parseMessage(task);
                rocketMQProducer.send(msg);

                task.setStatus(1);
                outboxTaskMapper.updateById(task);
                log.info("补偿成功: businessKey={}", task.getBusinessKey());

            } catch (Exception e) {
                int retryCount = task.getRetryCount() + 1;
                task.setRetryCount(retryCount);
                task.setFailReason(e.getMessage());

                if (retryCount >= task.getMaxRetry()) {
                    task.setStatus(2);
                    log.error("补偿失败(已达最大重试): businessKey={}", task.getBusinessKey());
                } else {
                    int delayMinutes = (int) Math.pow(2, retryCount);
                    task.setNextRetryTime(LocalDateTime.now().plusMinutes(delayMinutes));
                    log.warn("补偿失败(将重试): businessKey={}, retry={}, nextRetry={}",
                            task.getBusinessKey(), retryCount, task.getNextRetryTime());
                }
                outboxTaskMapper.updateById(task);
            }
        }
    }

    private CouponMessage parseMessage(OutboxTask task) {
        CouponMessage msg = new CouponMessage();
        msg.setBusinessKey(task.getBusinessKey());
        return msg;
    }
}
