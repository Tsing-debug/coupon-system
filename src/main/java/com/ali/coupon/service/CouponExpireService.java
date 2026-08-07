package com.ali.coupon.service;

import com.ali.coupon.common.enums.CouponStatus;
import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponExpireService {

    private static final Logger log = LoggerFactory.getLogger(CouponExpireService.class);

    private final UserCouponMapper userCouponMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponExpireService(UserCouponMapper userCouponMapper,
                               RedissonClient redissonClient,
                               RedisTemplate<String, Object> redisTemplate) {
        this.userCouponMapper = userCouponMapper;
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }

    public void handleExpire(Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            log.warn("过期处理: 券不存在 couponId={}", couponId);
            return;
        }

        int currentStatus = coupon.getStatus();

        if (currentStatus == CouponStatus.PENDING.getCode()) {
            expirePending(coupon);
        } else if (currentStatus == CouponStatus.LOCKED.getCode()) {
            expireLocked(coupon);
        } else {
            log.debug("过期处理: 券已不在待处理状态 couponId={}, status={}", couponId, currentStatus);
        }

        clearCache(coupon);
    }

    private void expirePending(UserCoupon coupon) {
        int updated = userCouponMapper.updateStatus(
                coupon.getId(),
                CouponStatus.EXPIRED.getCode(),
                CouponStatus.PENDING.getCode()
        );

        if (updated > 0) {
            log.info("券过期处理完成: couponId={}, userId={}, PENDING→EXPIRED",
                    coupon.getId(), coupon.getUserId());
        }
    }

    private void expireLocked(UserCoupon coupon) {
        RLock lock = redissonClient.getLock("lock:coupon:" + coupon.getId());

        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("过期处理: 释放仍持有的锁 couponId={}", coupon.getId());
        }

        int updated = userCouponMapper.updateStatus(
                coupon.getId(),
                CouponStatus.PENDING.getCode(),
                CouponStatus.LOCKED.getCode()
        );

        if (updated > 0) {
            log.info("锁券超时回滚: couponId={}, userId={}, LOCKED→PENDING",
                    coupon.getId(), coupon.getUserId());
        }
    }

    private void clearCache(UserCoupon coupon) {
        redisTemplate.delete("coupon:detail:" + coupon.getId());
        redisTemplate.opsForZSet()
                .remove("user:coupon:zset:" + coupon.getUserId(), coupon.getId().toString());
        log.debug("过期处理: 清除缓存和ZSet索引 couponId={}", coupon.getId());
    }
}
