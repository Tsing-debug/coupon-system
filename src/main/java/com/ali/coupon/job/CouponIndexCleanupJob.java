package com.ali.coupon.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CouponIndexCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CouponIndexCleanupJob.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CouponIndexCleanupJob(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每小时清理一次过期的 ZSet 条目
     * 过期判定: Score (过期时间戳ms) < 当前时间戳
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanExpiredZSetEntries() {
        log.info("开始清理过期ZSet索引...");

        Set<String> keys = redisTemplate.keys("user:coupon:zset:*");
        if (keys == null || keys.isEmpty()) {
            log.info("无ZSet索引需要清理");
            return;
        }

        long now = System.currentTimeMillis();
        long totalRemoved = 0;

        for (String key : keys) {
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
            if (removed != null && removed > 0) {
                totalRemoved += removed;
            }
        }

        log.info("过期ZSet清理完成: 扫描{}个key, 移除{}条过期记录", keys.size(), totalRemoved);
    }
}
