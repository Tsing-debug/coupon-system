package com.ali.coupon.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ali.coupon.dto.BatchUserDto;
import com.ali.coupon.mq.BatchCouponMessage;
import com.ali.coupon.mq.BatchRocketMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CouponDataListener extends AnalysisEventListener<BatchUserDto> {

    private static final Logger log = LoggerFactory.getLogger(CouponDataListener.class);
    private static final int BATCH_LIMIT = 500;

    private final String jobId;
    private final BatchRocketMQProducer producer;
    private final List<Long> userIdBuffer = new ArrayList<>(BATCH_LIMIT);

    // 提取第一行的模板/活动信息（假定同文件内一致）
    private Long templateId;
    private Long activityId;
    private String shopNumber;
    private String couponAmount;
    private int batchIndex = 0;
    private int totalUsers = 0;

    public CouponDataListener(String jobId, BatchRocketMQProducer producer) {
        this.jobId = jobId;
        this.producer = producer;
    }

    @Override
    public void invoke(BatchUserDto data, AnalysisContext context) {
        if (templateId == null) {
            templateId = data.getTemplateId();
            activityId = data.getActivityId();
            shopNumber = data.getShopNumber() != null ? data.getShopNumber() : "DEFAULT";
            couponAmount = data.getCouponAmount() != null ? data.getCouponAmount() : "0.00";
        }

        userIdBuffer.add(data.getUserId());
        totalUsers++;

        if (userIdBuffer.size() >= BATCH_LIMIT) {
            flushBuffer();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!userIdBuffer.isEmpty()) {
            flushBuffer();
        }
        log.info("文件解析完成: jobId={}, 总用户数={}, 总批次={}", jobId, totalUsers, batchIndex);
    }

    private void flushBuffer() {
        List<Long> batch = new ArrayList<>(userIdBuffer);
        userIdBuffer.clear();

        BatchCouponMessage msg = new BatchCouponMessage();
        msg.setJobId(jobId);
        msg.setTemplateId(templateId);
        msg.setActivityId(activityId);
        msg.setShopNumber(shopNumber);
        msg.setCouponAmount(couponAmount);
        msg.setUserIdList(batch);
        msg.setBatchIndex(batchIndex++);
        msg.setTotalBatches(-1);

        producer.send(msg);
        log.debug("批次发送: jobId={}, batchIndex={}, size={}", jobId, batchIndex - 1, batch.size());
    }

    public int getTotalUsers() { return totalUsers; }
    public int getTotalBatches() { return batchIndex; }
    public String getJobId() { return jobId; }
}
