package com.ali.coupon.job;

import com.ali.coupon.entity.BatchJobRecord;
import com.ali.coupon.service.BatchJobRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BatchJobRetryJob {

    private static final Logger log = LoggerFactory.getLogger(BatchJobRetryJob.class);

    private static final int MAX_RETRY = 3;

    private final BatchJobRecordService jobRecordService;

    public BatchJobRetryJob(BatchJobRecordService jobRecordService) {
        this.jobRecordService = jobRecordService;
    }

    /**
     * 每 5 分钟扫描未完成的任务
     * 状态 0(进行中) 超过 30 分钟 → 标记为失败待重试
     * 状态 3(失败待重试) → 触发重试
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void scanStaleJobs() {
        log.debug("BatchJobRetryJob 扫描开始...");

        // 扫描超时的"进行中"任务（超过30分钟仍未完成）
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<BatchJobRecord> staleJobs = jobRecordService.list(
                new LambdaQueryWrapper<BatchJobRecord>()
                        .eq(BatchJobRecord::getStatus, 0)
                        .lt(BatchJobRecord::getCreatedAt, threshold)
        );

        for (BatchJobRecord job : staleJobs) {
            log.warn("任务超时, 标记为失败待重试: jobId={}, total={}, success={}, fail={}",
                    job.getJobId(), job.getTotalCount(), job.getSuccessCount(), job.getFailCount());
            job.setStatus(3);
            jobRecordService.updateById(job);
        }

        // 扫描待重试的任务
        List<BatchJobRecord> retryJobs = jobRecordService.list(
                new LambdaQueryWrapper<BatchJobRecord>()
                        .eq(BatchJobRecord::getStatus, 3)
        );

        for (BatchJobRecord job : retryJobs) {
            log.warn("任务需要人工介入或重试: jobId={}, total={}, success={}, fail={}",
                    job.getJobId(), job.getTotalCount(), job.getSuccessCount(), job.getFailCount());
        }
    }
}
