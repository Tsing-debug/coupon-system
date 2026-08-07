package com.ali.coupon.controller;

import com.alibaba.excel.EasyExcel;
import com.ali.coupon.dto.BatchUploadResult;
import com.ali.coupon.dto.BatchUserDto;
import com.ali.coupon.entity.BatchJobRecord;
import com.ali.coupon.listener.CouponDataListener;
import com.ali.coupon.mq.BatchRocketMQProducer;
import com.ali.coupon.service.BatchJobRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/batch")
public class BatchUploadController {

    private static final Logger log = LoggerFactory.getLogger(BatchUploadController.class);

    private final BatchRocketMQProducer batchProducer;
    private final BatchJobRecordService jobRecordService;

    public BatchUploadController(BatchRocketMQProducer batchProducer,
                                 BatchJobRecordService jobRecordService) {
        this.batchProducer = batchProducer;
        this.jobRecordService = jobRecordService;
    }

    @PostMapping("/upload")
    public BatchUploadResult upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "operator", required = false) String operator) {
        String jobId = UUID.randomUUID().toString().replace("-", "");

        CouponDataListener listener = new CouponDataListener(jobId, batchProducer);

        try {
            EasyExcel.read(file.getInputStream(), BatchUserDto.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            log.error("文件读取失败: jobId={}", jobId, e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }

        BatchJobRecord record = new BatchJobRecord();
        record.setJobId(jobId);
        record.setOperator(operator);
        record.setTotalCount(listener.getTotalUsers());
        record.setSuccessCount(0);
        record.setFailCount(0);
        record.setStatus(0);
        jobRecordService.save(record);

        log.info("批量任务已提交: jobId={}, 总用户={}, 批次数={}",
                jobId, listener.getTotalUsers(), listener.getTotalBatches());

        return BatchUploadResult.success(jobId, listener.getTotalUsers(), 0, listener.getTotalUsers());
    }
}
