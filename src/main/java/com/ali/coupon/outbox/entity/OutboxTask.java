package com.ali.coupon.outbox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("outbox_task")
public class OutboxTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务类型(分片键): COUPON_ISSUE/COUPON_USE/COUPON_REFUND */
    private String taskType;

    /** 业务幂等键(如 user_id:template_id:activity_id) */
    private String businessKey;

    /** 消息体(JSON) */
    private String payload;

    /** 任务状态: 0-待发送 1-已发送 2-发送失败 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 失败原因 */
    private String failReason;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
