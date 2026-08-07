package com.ali.coupon.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class VerificationJob {

    private static final Logger log = LoggerFactory.getLogger(VerificationJob.class);

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    public VerificationJob(DataSource dataSource, RedisTemplate<String, Object> redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每 10 分钟运行一次数据对账
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void runReconciliation() {
        log.info("========== 数据对账开始 ==========");

        List<String> checks = new ArrayList<>();

        try {
            checkZSetVsDB(checks);
            checkOversell(checks);
            checkZombieLocks(checks);
        } catch (Exception e) {
            log.error("对账异常", e);
            checks.add("FAIL - 对账异常: " + e.getMessage());
        }

        boolean allPass = checks.stream().noneMatch(c -> c.startsWith("FAIL"));
        String result = allPass ? "PASS" : "FAIL";
        log.info("========== 对账结果: {} ==========", result);
        for (String check : checks) {
            log.info("  {}", check);
        }
    }

    /**
     * 检查 ZSet 数量 vs DB 待使用+锁券中数量
     */
    private void checkZSetVsDB(List<String> checks) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            // 扫描 Redis ZSet keys
            Set<String> zsetKeys = redisTemplate.keys("user:coupon:zset:*");
            if (zsetKeys == null || zsetKeys.isEmpty()) {
                checks.add("PASS - ZSet 为空，跳过");
                return;
            }

            int totalMismatch = 0;
            for (String zsetKey : zsetKeys) {
                String userId = zsetKey.replace("user:coupon:zset:", "");
                Long zsetSize = redisTemplate.opsForZSet().size(zsetKey);
                long dbCount = 0;

                var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM user_coupon WHERE user_id = " + userId
                    + " AND status IN (1, 2)");
                if (rs.next()) {
                    dbCount = rs.getLong(1);
                }

                if (zsetSize != null && zsetSize != dbCount) {
                    totalMismatch++;
                    log.warn("ZSet/DB不一致: userId={}, ZSet={}, DB={}", userId, zsetSize, dbCount);
                }
            }

            if (totalMismatch == 0) {
                checks.add("PASS - ZSet/DB一致性: 扫描 " + zsetKeys.size() + " 个key，全部一致");
            } else {
                checks.add("FAIL - ZSet/DB不一致: " + totalMismatch + " 个key不匹配");
            }
        } catch (Exception e) {
            checks.add("FAIL - ZSet/DB检查异常: " + e.getMessage());
        }
    }

    /**
     * 超卖检查
     */
    private void checkOversell(List<String> checks) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            var rs = stmt.executeQuery(
                "SELECT activity_id, COUNT(*) cnt FROM user_coupon " +
                "WHERE status IN (1, 2, 3) " +
                "GROUP BY activity_id HAVING cnt > 100000"
            );

            boolean hasIssue = false;
            while (rs.next()) {
                hasIssue = true;
                log.warn("疑似超卖: activityId={}, count={}", rs.getLong(1), rs.getLong(2));
            }

            if (!hasIssue) {
                checks.add("PASS - 超卖检查: 无异常");
            } else {
                checks.add("FAIL - 超卖检查: 发现疑似超卖");
            }
        } catch (Exception e) {
            checks.add("FAIL - 超卖检查异常: " + e.getMessage());
        }
    }

    /**
     * 僵尸锁检查：lock_time < 10分钟前 且 status=2
     */
    private void checkZombieLocks(List<String> checks) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            var rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM user_coupon " +
                "WHERE status = 2 AND lock_time < DATE_SUB(NOW(), INTERVAL 10 MINUTE)"
            );

            long zombieCount = 0;
            if (rs.next()) {
                zombieCount = rs.getLong(1);
            }

            if (zombieCount == 0) {
                checks.add("PASS - 僵尸锁检查: 无僵尸锁");
            } else {
                checks.add("FAIL - 僵尸锁检查: 发现 " + zombieCount + " 个僵尸锁");
            }
        } catch (Exception e) {
            checks.add("FAIL - 僵尸锁检查异常: " + e.getMessage());
        }
    }
}
