-- ============================================
-- Ali Coupon System - 初始化建表脚本
-- 字符集: utf8mb4, 引擎: InnoDB ROW_FORMAT=DYNAMIC
-- ============================================

-- ----------------------------
-- 1. 优惠券模板表 (merchant 领域)
-- 分片键: shop_number
-- ----------------------------
CREATE TABLE `coupon_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_number` varchar(32) NOT NULL COMMENT '商家号(分片键)',
    `template_name` varchar(128) NOT NULL COMMENT '模板名称',
    `discount_type` tinyint NOT NULL COMMENT '优惠类型: 1-满减 2-折扣 3-立减',
    `discount_value` decimal(10,2) NOT NULL COMMENT '优惠值(满减/立减为金额,折扣为折扣率如0.85)',
    `threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '满减门槛金额',
    `total_quantity` int NOT NULL DEFAULT 0 COMMENT '总发行量',
    `used_quantity` int NOT NULL DEFAULT 0 COMMENT '已领取量',
    `per_user_limit` int NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `valid_start_time` datetime NOT NULL COMMENT '模板有效期起始',
    `valid_end_time` datetime NOT NULL COMMENT '模板有效期截止',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '模板状态: 1-启用 2-停用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_status` (`shop_number`, `status`),
    KEY `idx_shop_valid` (`shop_number`, `valid_start_time`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='优惠券模板表';

-- ----------------------------
-- 2. 用户优惠券表 (coupon 领域)
-- 分片键: user_id
-- 冗余字段: shop_number (避免跨库JOIN)
-- 幂等键: uk_user_template_batch
-- ----------------------------
CREATE TABLE `user_coupon` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` bigint NOT NULL COMMENT '用户ID(分片键)',
    `template_id` bigint NOT NULL COMMENT '模板ID',
    `shop_number` varchar(32) NOT NULL COMMENT '冗余商家号(避免跨库JOIN)',
    `batch_no` varchar(64) NOT NULL COMMENT '幂等控制批次号',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '券状态: 1-待使用 2-锁券中 3-已核销 4-已退款 5-已过期',
    `coupon_amount` decimal(10,2) NOT NULL COMMENT '券面金额',
    `threshold_amount` decimal(10,2) DEFAULT NULL COMMENT '使用门槛金额',
    `valid_start_time` datetime NOT NULL COMMENT '券有效期起始',
    `valid_end_time` datetime NOT NULL COMMENT '券有效期截止',
    `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
    `use_time` datetime DEFAULT NULL COMMENT '核销时间',
    `refund_time` datetime DEFAULT NULL COMMENT '退款时间',
    `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领券时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_template_batch` (`user_id`, `template_id`, `batch_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_user_valid` (`user_id`, `valid_end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户优惠券表';

-- ----------------------------
-- 3. 兑换活动表 (activity 领域)
-- 分片键: shop_number
-- ----------------------------
CREATE TABLE `exchange_activity` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_number` varchar(32) NOT NULL COMMENT '商家号(分片键)',
    `activity_name` varchar(128) NOT NULL COMMENT '活动名称',
    `activity_desc` varchar(512) DEFAULT NULL COMMENT '活动描述',
    `template_id` bigint NOT NULL COMMENT '关联模板ID',
    `total_stock` int NOT NULL DEFAULT 0 COMMENT '活动总库存',
    `used_stock` int NOT NULL DEFAULT 0 COMMENT '已兑换数量',
    `per_user_limit` int NOT NULL DEFAULT 1 COMMENT '每人限兑次数',
    `start_time` datetime NOT NULL COMMENT '活动开始时间',
    `end_time` datetime NOT NULL COMMENT '活动结束时间',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '活动状态: 1-未开始 2-进行中 3-已结束 4-已关闭',
    `rule_config` json DEFAULT NULL COMMENT '活动规则配置(JSON)',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_shop_status` (`shop_number`, `status`),
    KEY `idx_shop_time` (`shop_number`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='兑换活动表';

-- ----------------------------
-- 4. 本地消息表 (outbox 领域)
-- 分片键: task_type
-- 幂等键: uk_business_key
-- 联合索引: idx_status_retry (支撑 XXL-Job 定时扫表)
-- ----------------------------
CREATE TABLE `outbox_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_type` varchar(32) NOT NULL COMMENT '任务类型(分片键): COUPON_ISSUE/COUPON_USE/COUPON_REFUND',
    `business_key` varchar(128) NOT NULL COMMENT '业务幂等键(如 user_id:template_id:activity_id)',
    `payload` json NOT NULL COMMENT '消息体(JSON)',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '任务状态: 0-待发送 1-已发送 2-发送失败',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry` int NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    `next_retry_time` datetime NOT NULL COMMENT '下次重试时间',
    `fail_reason` varchar(512) DEFAULT NULL COMMENT '失败原因',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='本地消息表(发件箱)';

-- ----------------------------
-- 5. 批量发券任务记录表 (batch 领域)
-- 用途: 记录批量发券任务的状态和进度
-- ----------------------------
CREATE TABLE `batch_job_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id` varchar(64) NOT NULL COMMENT '唯一任务ID(雪花ID)',
    `operator` varchar(32) DEFAULT NULL COMMENT '操作人',
    `total_count` int NOT NULL DEFAULT 0 COMMENT '总用户数',
    `success_count` int NOT NULL DEFAULT 0 COMMENT '成功数',
    `fail_count` int NOT NULL DEFAULT 0 COMMENT '失败数',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成 2-部分失败 3-失败待重试',
    `fail_detail` json DEFAULT NULL COMMENT '失败详情(用户列表/错误栈)',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_id` (`job_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='批量发券任务记录表';
