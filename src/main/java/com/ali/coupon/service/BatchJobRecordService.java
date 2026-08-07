package com.ali.coupon.service;

import com.ali.coupon.entity.BatchJobRecord;
import com.ali.coupon.mapper.BatchJobRecordMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BatchJobRecordService extends ServiceImpl<BatchJobRecordMapper, BatchJobRecord> {

    public void markCompleted(String jobId) {
        lambdaUpdate()
                .eq(BatchJobRecord::getJobId, jobId)
                .set(BatchJobRecord::getStatus, 1)
                .update();
    }

    public void markPartialFail(String jobId) {
        lambdaUpdate()
                .eq(BatchJobRecord::getJobId, jobId)
                .set(BatchJobRecord::getStatus, 2)
                .update();
    }

    public void incrementSuccess(String jobId) {
        lambdaUpdate()
                .eq(BatchJobRecord::getJobId, jobId)
                .setSql("success_count = success_count + 1")
                .update();
    }

    public void incrementFail(String jobId) {
        lambdaUpdate()
                .eq(BatchJobRecord::getJobId, jobId)
                .setSql("fail_count = fail_count + 1")
                .update();
    }
}
