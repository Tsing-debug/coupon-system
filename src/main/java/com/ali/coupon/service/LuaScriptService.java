package com.ali.coupon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LuaScriptService {

    private static final Logger log = LoggerFactory.getLogger(LuaScriptService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> exchangeLuaScript;

    public LuaScriptService(RedisTemplate<String, Object> redisTemplate,
                            DefaultRedisScript<Long> exchangeLuaScript) {
        this.redisTemplate = redisTemplate;
        this.exchangeLuaScript = exchangeLuaScript;
    }

    /**
     * 执行兑换 Lua 脚本
     * @return 1=成功, -1=库存不足, -2=重复领取
     */
    public Long executeExchange(Long templateId, Long userId, Long activityId) {
        String stockKey = "stock:template:" + templateId;
        String userActKey = "user_act:" + userId + ":" + activityId;

        List<String> keys = List.of(stockKey, userActKey);
        Long result = redisTemplate.execute(exchangeLuaScript, keys, userId.toString());

        log.info("Lua exchange result: {}, templateId={}, userId={}, activityId={}",
                result, templateId, userId, activityId);
        return result;
    }
}
