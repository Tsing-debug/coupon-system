package com.ali.coupon.service;

import com.ali.coupon.common.enums.CouponStatus;
import com.ali.coupon.common.guard.StateMachineGuard;
import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponUseService {

    private static final Logger log = LoggerFactory.getLogger(CouponUseService.class);

    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponUseService(UserCouponMapper userCouponMapper,
                            RedisTemplate<String, Object> redisTemplate) {
        this.userCouponMapper = userCouponMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public boolean useCoupon(Long userId, Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("券不存在: " + couponId);
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("券不属于该用户");
        }

        CouponStatus from = CouponStatus.fromCode(coupon.getStatus());
        CouponStatus to = CouponStatus.USED;

        if (!StateMachineGuard.canTransition(from, to)) {
            throw new IllegalStateException(
                String.format("当前状态无法核销: couponId=%d, status=%s(%d)",
                    couponId, from.getDesc(), from.getCode()));
        }

        int updated = userCouponMapper.useCouponById(
                couponId, userId,
                CouponStatus.USED.getCode(),
                from.getCode()
        );

        if (updated > 0) {
            clearCouponCache(couponId, userId);
            log.info("核销成功: userId={}, couponId={}", userId, couponId);
            return true;
        }

        log.warn("核销失败(状态已变更): userId={}, couponId={}", userId, couponId);
        return false;
    }

    @Transactional
    public boolean refundCoupon(Long userId, Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("券不存在: " + couponId);
        }
        if (!coupon.getUserId().equals(userId)) {
            throw new IllegalArgumentException("券不属于该用户");
        }

        CouponStatus from = CouponStatus.fromCode(coupon.getStatus());
        CouponStatus to = CouponStatus.REFUNDED;

        // 退款允许从 PENDING(1) 或 LOCKED(2) 流转到 REFUNDED(4)
        if (from != CouponStatus.PENDING && from != CouponStatus.LOCKED) {
            throw new IllegalStateException(
                String.format("当前状态无法退款: couponId=%d, status=%s(%d)",
                    couponId, from.getDesc(), from.getCode()));
        }

        int updated = userCouponMapper.refundCouponById(couponId, userId);

        if (updated > 0) {
            clearCouponCache(couponId, userId);
            log.info("退款成功: userId={}, couponId={}", userId, couponId);
            return true;
        }

        log.warn("退款失败(状态已变更): userId={}, couponId={}", userId, couponId);
        return false;
    }

    private void clearCouponCache(Long couponId, Long userId) {
        redisTemplate.delete("coupon:detail:" + couponId);
        redisTemplate.opsForZSet().remove("user:coupon:zset:" + userId, couponId.toString());
        log.debug("清除券缓存: couponId={}, userId={}", couponId, userId);
    }
}
