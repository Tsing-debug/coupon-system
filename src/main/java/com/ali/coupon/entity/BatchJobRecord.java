package com.ali.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("batch_job_record")
public class BatchJobRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一任务ID(雪花ID) */
    private String jobId;

    /** 操作人 */
    private String operator;

    /** 总用户数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 状态: 0-进行中 1-已完成 2-部分失败 3-失败待重试 */
    private Integer status;

    /** 失败详情(JSON) */
    private String failDetail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
