package com.ali.coupon.mq;

import java.io.Serializable;
import java.util.List;

public class BatchCouponMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String jobId;
    private Long templateId;
    private Long activityId;
    private String shopNumber;
    private String couponAmount;
    private List<Long> userIdList;
    private int batchIndex;
    private int totalBatches;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getShopNumber() { return shopNumber; }
    public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }

    public String getCouponAmount() { return couponAmount; }
    public void setCouponAmount(String couponAmount) { this.couponAmount = couponAmount; }

    public List<Long> getUserIdList() { return userIdList; }
    public void setUserIdList(List<Long> userIdList) { this.userIdList = userIdList; }

    public int getBatchIndex() { return batchIndex; }
    public void setBatchIndex(int batchIndex) { this.batchIndex = batchIndex; }

    public int getTotalBatches() { return totalBatches; }
    public void setTotalBatches(int totalBatches) { this.totalBatches = totalBatches; }
}
