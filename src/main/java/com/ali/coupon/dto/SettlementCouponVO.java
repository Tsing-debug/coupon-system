package com.ali.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementCouponVO {

    private Long couponId;
    private Long templateId;
    private String shopNumber;
    private String templateName;
    private Integer discountType;
    private BigDecimal couponAmount;
    private BigDecimal thresholdAmount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime validEndTime;

    private BigDecimal calculatedDiscount;
    private boolean calcError;

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getShopNumber() { return shopNumber; }
    public void setShopNumber(String shopNumber) { this.shopNumber = shopNumber; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public Integer getDiscountType() { return discountType; }
    public void setDiscountType(Integer discountType) { this.discountType = discountType; }

    public BigDecimal getCouponAmount() { return couponAmount; }
    public void setCouponAmount(BigDecimal couponAmount) { this.couponAmount = couponAmount; }

    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusDesc() { return statusDesc; }
    public void setStatusDesc(String statusDesc) { this.statusDesc = statusDesc; }

    public LocalDateTime getValidEndTime() { return validEndTime; }
    public void setValidEndTime(LocalDateTime validEndTime) { this.validEndTime = validEndTime; }

    public BigDecimal getCalculatedDiscount() { return calculatedDiscount; }
    public void setCalculatedDiscount(BigDecimal calculatedDiscount) { this.calculatedDiscount = calculatedDiscount; }

    public boolean isCalcError() { return calcError; }
    public void setCalcError(boolean calcError) { this.calcError = calcError; }
}
