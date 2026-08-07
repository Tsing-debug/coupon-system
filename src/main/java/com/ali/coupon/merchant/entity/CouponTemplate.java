package com.ali.coupon.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家号(分片键) */
    private String shopNumber;

    /** 模板名称 */
    private String templateName;

    /** 优惠类型: 1-满减 2-折扣 3-立减 */
    private Integer discountType;

    /** 优惠值 */
    private BigDecimal discountValue;

    /** 满减门槛金额 */
    private BigDecimal thresholdAmount;

    /** 总发行量 */
    private Integer totalQuantity;

    /** 已领取量 */
    private Integer usedQuantity;

    /** 每人限领数量 */
    private Integer perUserLimit;

    /** 模板有效期起始 */
    private LocalDateTime validStartTime;

    /** 模板有效期截止 */
    private LocalDateTime validEndTime;

    /** 模板状态: 1-启用 2-停用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
