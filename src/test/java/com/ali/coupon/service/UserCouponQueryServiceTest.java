package com.ali.coupon.service;

import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.dto.SettlementCouponVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 结算查询端到端测试
 * 需要本地 Redis 运行
 */
@SpringBootTest
class UserCouponQueryServiceTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserCouponQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long TEST_USER_ID = 20001L;

    @BeforeEach
    void setUp() {
        String zsetKey = "user:coupon:zset:" + TEST_USER_ID;
        redisTemplate.delete(zsetKey);
        for (int i = 0; i < 5; i++) {
            redisTemplate.delete("coupon:detail:" + (10001L + i));
        }
    }

    @Test
    void shouldReturnEmptyWhenNoCoupons() {
        List<SettlementCouponVO> result = queryService.queryForSettlement(TEST_USER_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFilterExpiredCoupons() throws Exception {
        // 写入1张未过期 + 1张已过期的 ZSet 条目
        String zsetKey = "user:coupon:zset:" + TEST_USER_ID;

        long futureExpire = System.currentTimeMillis() + 86400000L; // 1天后
        long pastExpire = System.currentTimeMillis() - 86400000L;   // 1天前

        redisTemplate.opsForZSet().add(zsetKey, "10001", futureExpire);
        redisTemplate.opsForZSet().add(zsetKey, "10002", pastExpire);

        // 写对应的 detail 缓存
        SettlementCouponVO validVo = buildTestVO(10001L);
        SettlementCouponVO expiredVo = buildTestVO(10002L);
        queryService.cacheDetail(validVo);
        queryService.cacheDetail(expiredVo);

        List<SettlementCouponVO> result = queryService.queryForSettlement(TEST_USER_ID);

        assertEquals(1, result.size());
        assertEquals(10001L, result.get(0).getCouponId());
    }

    @Test
    void shouldWriteAndQueryCouponDetail() throws Exception {
        SettlementCouponVO vo = buildTestVO(30001L);
        queryService.cacheDetail(vo);

        String key = "coupon:detail:30001";
        String cached = (String) redisTemplate.opsForValue().get(key);
        assertNotNull(cached);

        SettlementCouponVO parsed = objectMapper.readValue(cached, SettlementCouponVO.class);
        assertEquals(30001L, parsed.getCouponId());
    }

    @Test
    void shouldWriteZSetIndex() {
        Date expireDate = Date.from(LocalDateTime.now().plusDays(30)
                .atZone(ZoneId.systemDefault()).toInstant());

        queryService.addUserCouponIndex(TEST_USER_ID, 40001L, expireDate);

        String zsetKey = "user:coupon:zset:" + TEST_USER_ID;
        Long size = redisTemplate.opsForZSet().size(zsetKey);
        assertEquals(1L, size);

        Double score = redisTemplate.opsForZSet().score(zsetKey, "40001");
        assertNotNull(score);
        assertEquals(expireDate.getTime(), score, 1000);
    }

    private SettlementCouponVO buildTestVO(Long couponId) {
        SettlementCouponVO vo = new SettlementCouponVO();
        vo.setCouponId(couponId);
        vo.setTemplateId(1L);
        vo.setShopNumber("SHOP001");
        vo.setTemplateName("测试模板");
        vo.setDiscountType(1);
        vo.setCouponAmount(new BigDecimal("10.00"));
        vo.setThresholdAmount(new BigDecimal("100.00"));
        vo.setStatus(1);
        vo.setStatusDesc("待使用");
        vo.setValidEndTime(LocalDateTime.now().plusDays(30));
        return vo;
    }
}
