package com.ali.coupon.service;

import com.ali.coupon.common.enums.CouponStatus;
import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import com.ali.coupon.dto.SettlementCouponVO;
import com.ali.coupon.merchant.entity.CouponTemplate;
import com.ali.coupon.merchant.mapper.CouponTemplateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserCouponQueryService {

    private static final Logger log = LoggerFactory.getLogger(UserCouponQueryService.class);

    private static final int MAX_COUPONS = 100;
    private static final int CACHE_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    private final Executor couponCalcExecutor;

    public UserCouponQueryService(RedisTemplate<String, Object> redisTemplate,
                                  UserCouponMapper userCouponMapper,
                                  CouponTemplateMapper templateMapper,
                                  ObjectMapper objectMapper,
                                  @org.springframework.beans.factory.annotation.Qualifier("couponCalcExecutor") Executor couponCalcExecutor) {
        this.redisTemplate = redisTemplate;
        this.userCouponMapper = userCouponMapper;
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
        this.couponCalcExecutor = couponCalcExecutor;
    }

    /**
     * 结算页查询用户可用券
     * 1. ZSet 按 Score 范围查未过期的 couponId (Score = 过期时间戳 ms)
     * 2. Pipeline 批量拉 coupon:detail:{couponId} 缓存
     * 3. 缓存未命中时降级查 DB，并异步回写缓存
     */
    public List<SettlementCouponVO> queryForSettlement(Long userId) {
        String zsetKey = "user:coupon:zset:" + userId;

        long now = System.currentTimeMillis();
        Set<Object> couponIdObjs = redisTemplate.opsForZSet()
                .rangeByScore(zsetKey, now, Double.MAX_VALUE, 0, MAX_COUPONS);

        if (couponIdObjs == null || couponIdObjs.isEmpty()) {
            log.debug("用户无可用券: userId={}", userId);
            return Collections.emptyList();
        }

        List<String> couponIdStrs = couponIdObjs.stream()
                .map(String::valueOf).collect(Collectors.toList());

        List<String> cacheKeys = couponIdStrs.stream()
                .map(id -> "coupon:detail:" + id).collect(Collectors.toList());

        long start = System.currentTimeMillis();

        List<Object> cachedResults = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    for (String key : cacheKeys) {
                        connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
                    }
                    return null;
                }
        );

        long elapsed = System.currentTimeMillis() - start;

        List<SettlementCouponVO> result = new ArrayList<>();
        List<String> missedIds = new ArrayList<>();

        for (int i = 0; i < couponIdStrs.size(); i++) {
            Object cached = cachedResults.get(i);
            if (cached != null) {
                try {
                    SettlementCouponVO vo = objectMapper.readValue((String) cached, SettlementCouponVO.class);
                    result.add(vo);
                } catch (JsonProcessingException e) {
                    missedIds.add(couponIdStrs.get(i));
                }
            } else {
                missedIds.add(couponIdStrs.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            log.warn("缓存未命中 {} 条, userId={}, 降级查DB", missedIds.size(), userId);
            List<SettlementCouponVO> dbResults = fallbackToDb(missedIds);
            result.addAll(dbResults);
        }

        log.info("结算查询完成: userId={}, 结果={}, 命中={}/{}, Pipeline耗时={}ms",
                userId, result.size(),
                couponIdStrs.size() - missedIds.size(), couponIdStrs.size(), elapsed);

        return result;
    }

    /**
     * 缓存未命中时降级查 DB
     */
    private List<SettlementCouponVO> fallbackToDb(List<String> couponIds) {
        List<SettlementCouponVO> result = new ArrayList<>();
        for (String idStr : couponIds) {
            try {
                Long couponId = Long.parseLong(idStr);
                UserCoupon coupon = userCouponMapper.selectById(couponId);
                if (coupon != null) {
                    SettlementCouponVO vo = buildFromEntity(coupon);
                    result.add(vo);
                    // 异步回写缓存（简化：直接同步写）
                    cacheDetail(vo);
                }
            } catch (NumberFormatException e) {
                log.error("非法couponId: {}", idStr);
            }
        }
        return result;
    }

    private SettlementCouponVO buildFromEntity(UserCoupon coupon) {
        SettlementCouponVO vo = new SettlementCouponVO();
        vo.setCouponId(coupon.getId());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setShopNumber(coupon.getShopNumber());
        vo.setCouponAmount(coupon.getCouponAmount());
        vo.setThresholdAmount(coupon.getThresholdAmount());
        vo.setStatus(coupon.getStatus());
        vo.setStatusDesc(CouponStatus.fromCode(coupon.getStatus()).getDesc());
        vo.setValidEndTime(coupon.getValidEndTime());

        // 补充模板信息
        CouponTemplate template = templateMapper.selectById(coupon.getTemplateId());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setDiscountType(template.getDiscountType());
        }

        return vo;
    }

    /**
     * 写 coupon:detail:{couponId} 缓存
     */
    public void cacheDetail(SettlementCouponVO vo) {
        try {
            String key = "coupon:detail:" + vo.getCouponId();
            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("缓存券详情失败: couponId={}", vo.getCouponId(), e);
        }
    }

    /**
     * 写 ZSet 索引: user:coupon:zset:{userId}
     */
    public void addUserCouponIndex(Long userId, Long couponId, Date validEndTime) {
        String zsetKey = "user:coupon:zset:" + userId;
        redisTemplate.opsForZSet().add(zsetKey, couponId.toString(), validEndTime.getTime());
        log.debug("ZSet索引写入: userId={}, couponId={}, expireScore={}", userId, couponId, validEndTime.getTime());
    }

    /**
     * 清理过期的 ZSet 条目（由定时任务调用）
     */
    public void cleanExpired(Long userId) {
        String zsetKey = "user:coupon:zset:" + userId;
        redisTemplate.opsForZSet()
                .removeRangeByScore(zsetKey, 0, System.currentTimeMillis());
    }

    /**
     * CompletableFuture 并行计算优惠金额
     * 10张券并行耗时 ≈ 最慢单张，非串行累加
     */
    public List<SettlementCouponVO> enrichWithCalculatedDiscount(List<SettlementCouponVO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return rawList;
        }

        long start = System.currentTimeMillis();

        List<CompletableFuture<SettlementCouponVO>> futures = rawList.stream()
                .map(vo -> CompletableFuture.supplyAsync(() -> {
                    BigDecimal discount = doComplexDiscountCalc(vo);
                    vo.setCalculatedDiscount(discount);
                    return vo;
                }, couponCalcExecutor)
                .exceptionally(ex -> {
                    log.error("优惠计算失败, couponId={}, 降级为0", vo.getCouponId(), ex);
                    vo.setCalculatedDiscount(BigDecimal.ZERO);
                    vo.setCalcError(true);
                    return vo;
                }))
                .collect(Collectors.toList());

        List<SettlementCouponVO> result = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - start;
        log.info("并行优惠计算完成: 券数={}, 耗时={}ms", rawList.size(), elapsed);

        return result;
    }

    /**
     * 优惠计算逻辑（模拟：满减/折扣/立减）
     */
    private BigDecimal doComplexDiscountCalc(SettlementCouponVO vo) {
        if (vo.getDiscountType() == null) {
            return BigDecimal.ZERO;
        }
        return switch (vo.getDiscountType()) {
            case 1 -> vo.getCouponAmount();                              // 立减
            case 2 -> vo.getCouponAmount().multiply(new BigDecimal("0.1")); // 折扣
            case 3 -> {
                if (vo.getThresholdAmount() != null) {
                    yield vo.getCouponAmount();                           // 满减
                }
                yield BigDecimal.ZERO;
            }
            default -> BigDecimal.ZERO;
        };
    }
}
