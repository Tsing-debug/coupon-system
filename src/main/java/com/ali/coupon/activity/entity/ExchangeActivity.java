package com.ali.coupon.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange_activity")
public class ExchangeActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家号(分片键) */
    private String shopNumber;

    /** 活动名称 */
    private String activityName;

    /** 活动描述 */
    private String activityDesc;

    /** 关联模板ID */
    private Long templateId;

    /** 活动总库存 */
    private Integer totalStock;

    /** 已兑换数量 */
    private Integer usedStock;

    /** 每人限兑次数 */
    private Integer perUserLimit;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态: 1-未开始 2-进行中 3-已结束 4-已关闭 */
    private Integer status;

    /** 活动规则配置(JSON) */
    private String ruleConfig;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
