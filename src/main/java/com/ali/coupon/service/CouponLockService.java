package com.ali.coupon.service;

import com.ali.coupon.common.enums.CouponStatus;
import com.ali.coupon.common.guard.StateMachineGuard;
import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class CouponLockService {

    private static final Logger log = LoggerFactory.getLogger(CouponLockService.class);

    private final RedissonClient redissonClient;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponLockService(RedissonClient redissonClient,
                             UserCouponMapper userCouponMapper,
                             RedisTemplate<String, Object> redisTemplate) {
        this.redissonClient = redissonClient;
        this.userCouponMapper = userCouponMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public boolean lockCoupon(Long userId, Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("券不存在: " + couponId);
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("券不属于该用户");
        }
        if (!StateMachineGuard.canTransition(coupon.getStatus(), CouponStatus.LOCKED.getCode())) {
            throw new IllegalStateException(
                String.format("当前状态无法锁券: couponId=%d, status=%d", couponId, coupon.getStatus()));
        }

        RLock lock = redissonClient.getLock("lock:coupon:" + couponId);
        try {
            if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                try {
                    int updated = userCouponMapper.lockCouponById(
                            couponId, userId,
                            CouponStatus.LOCKED.getCode(),
                            CouponStatus.PENDING.getCode(),
                            coupon.getVersion()
                    );

                    if (updated > 0) {
                        redisTemplate.delete("coupon:detail:" + couponId);
                        log.info("锁券成功: userId={}, couponId={}", userId, couponId);
                        return true;
                    }

                    log.warn("锁券失败(版本冲突): userId={}, couponId={}, version={}",
                            userId, couponId, coupon.getVersion());
                    return false;

                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }

            log.warn("锁券失败(获取分布式锁超时): userId={}, couponId={}", userId, couponId);
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("锁券被中断: userId={}, couponId={}", userId, couponId);
            return false;
        }
    }
}
