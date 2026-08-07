package com.ali.coupon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lua 脚本单元测试
 * 需要本地 Redis 运行中 (docker-compose up -d redis)
 */
@SpringBootTest
class LuaScriptServiceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DefaultRedisScript<Long> exchangeLuaScript;

    private static final Long TEMPLATE_ID = 999L;
    private static final Long USER_ID = 100L;
    private static final Long ACTIVITY_ID = 888L;

    @BeforeEach
    void setUp() {
        String stockKey = "stock:template:" + TEMPLATE_ID;
        String userActKey = "user_act:" + USER_ID + ":" + ACTIVITY_ID;
        redisTemplate.delete(List.of(stockKey, userActKey));
        redisTemplate.opsForValue().set(stockKey, "50");
    }

    @Test
    void shouldReturn1WhenExchangeSuccess() {
        List<String> keys = List.of(
                "stock:template:" + TEMPLATE_ID,
                "user_act:" + 200L + ":" + ACTIVITY_ID);
        Long result = redisTemplate.execute(exchangeLuaScript, keys, "200");
        assertEquals(1L, result);
    }

    @Test
    void shouldReturnMinus1WhenStockInsufficient() {
        String stockKey = "stock:template:empty:" + TEMPLATE_ID;
        redisTemplate.opsForValue().set(stockKey, "0");

        List<String> keys = List.of(stockKey, "user_act:300:" + ACTIVITY_ID);
        Long result = redisTemplate.execute(exchangeLuaScript, keys, "300");
        assertEquals(-1L, result);
    }

    @Test
    void shouldReturnMinus2WhenDuplicateUser() {
        String stockKey = "stock:template:" + TEMPLATE_ID;
        String userActKey = "user_act:" + 400L + ":" + ACTIVITY_ID;

        List<String> keys = List.of(stockKey, userActKey);
        redisTemplate.execute(exchangeLuaScript, keys, "400");
        Long result = redisTemplate.execute(exchangeLuaScript, keys, "400");

        assertEquals(-2L, result);
    }

    @Test
    void shouldDecrementStockAtomically() {
        String stockKey = "stock:template:atomic:" + TEMPLATE_ID;
        String userActKey = "user_act:atomic:user:" + ACTIVITY_ID;
        redisTemplate.opsForValue().set(stockKey, "10");

        List<String> keys = List.of(stockKey, userActKey);
        Long result = redisTemplate.execute(exchangeLuaScript, keys, "500");
        assertEquals(1L, result);

        String remainingStock = (String) redisTemplate.opsForValue().get(stockKey);
        assertEquals("9", remainingStock);
    }
}
