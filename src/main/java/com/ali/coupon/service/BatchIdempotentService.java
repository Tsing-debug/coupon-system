package com.ali.coupon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BatchIdempotentService {

    private static final Logger log = LoggerFactory.getLogger(BatchIdempotentService.class);

    private static final String BATCH_KEY_PREFIX = "batch:job:";
    private static final int EXPIRE_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;

    public BatchIdempotentService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 利用 Redis Set SADD 返回值过滤已处理的用户
     * @return 未被处理的新用户列表
     */
    public List<Long> filterNewUsers(String jobId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        String key = BATCH_KEY_PREFIX + jobId + ":users";
        String[] members = userIds.stream().map(String::valueOf).toArray(String[]::new);
        Long addedCount = redisTemplate.opsForSet().add(key, (Object[]) members);

        redisTemplate.expire(key, EXPIRE_DAYS, TimeUnit.DAYS);

        log.info("批次去重: jobId={}, 本批={}, 新增={}, 重复={}",
                jobId, userIds.size(), addedCount, userIds.size() - (addedCount != null ? addedCount : 0));

        if (addedCount != null && addedCount == 0) {
            return Collections.emptyList();
        }

        return userIds;
    }

    /**
     * 检查某个 job 是否全部已处理
     */
    public boolean isAllProcessed(String jobId, List<Long> userIds) {
        String key = BATCH_KEY_PREFIX + jobId + ":users";
        List<Object> members = userIds.stream().map(String::valueOf).collect(Collectors.toList());
        List<Object> results = redisTemplate.opsForSet()
                .isMember(key, members.toArray());

        return results.stream().allMatch(r -> Boolean.TRUE.equals(r));
    }
}
