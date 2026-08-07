package com.ali.coupon.dto;

import jakarta.validation.constraints.NotNull;

public class ExchangeRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
}
