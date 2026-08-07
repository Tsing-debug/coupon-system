package com.ali.coupon.coupon.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID(分片键) */
    private Long userId;

    /** 模板ID */
    private Long templateId;

    /** 冗余商家号(避免跨库JOIN) */
    private String shopNumber;

    /** 幂等控制批次号 */
    private String batchNo;

    /** 券状态: 1-待使用 2-锁券中 3-已核销 4-已退款 5-已过期 */
    private Integer status;

    /** 券面金额 */
    private BigDecimal couponAmount;

    /** 使用门槛金额 */
    private BigDecimal thresholdAmount;

    /** 券有效期起始 */
    private LocalDateTime validStartTime;

    /** 券有效期截止 */
    private LocalDateTime validEndTime;

    /** 锁定时间 */
    private LocalDateTime lockTime;

    /** 核销时间 */
    private LocalDateTime useTime;

    /** 退款时间 */
    private LocalDateTime refundTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
