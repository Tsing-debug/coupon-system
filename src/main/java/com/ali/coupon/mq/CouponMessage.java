package com.ali.coupon.mq;

import java.io.Serializable;

public class CouponMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long templateId;
    private Long activityId;
    private String batchNo;
    private String businessKey;
    private String shopNumber;
    private String couponAmount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public String getShopNumber() { return shopNumber; }
    public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }

    public String getCouponAmount() { return couponAmount; }
    public void setCouponAmount(String couponAmount) { this.couponAmount = couponAmount; }
}
