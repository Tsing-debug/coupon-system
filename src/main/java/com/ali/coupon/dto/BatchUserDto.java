package com.ali.coupon.dto;

import com.alibaba.excel.annotation.ExcelProperty;

public class BatchUserDto {

    @ExcelProperty(index = 0)
    private Long userId;

    @ExcelProperty(index = 1)
    private Long templateId;

    @ExcelProperty(index = 2)
    private Long activityId;

    @ExcelProperty(index = 3)
    private String shopNumber;

    @ExcelProperty(index = 4)
    private String couponAmount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getShopNumber() { return shopNumber; }
    public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }

    public String getCouponAmount() { return couponAmount; }
    public void setCouponAmount(String couponAmount) { this.couponAmount = couponAmount; }
}
