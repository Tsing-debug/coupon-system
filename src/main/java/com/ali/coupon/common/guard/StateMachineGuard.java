package com.ali.coupon.common.guard;

import com.ali.coupon.common.enums.CouponStatus;

public final class StateMachineGuard {

    private StateMachineGuard() {
    }

    public static void validateTransition(CouponStatus from, CouponStatus to) {
        if (from == null) {
            throw new IllegalArgumentException("源状态不能为空");
        }
        if (to == null) {
            throw new IllegalArgumentException("目标状态不能为空");
        }
        from.transitionTo(to);
    }

    public static boolean canTransition(CouponStatus from, CouponStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return from.canTransitionTo(to);
    }

    public static boolean canTransition(int fromCode, int toCode) {
        try {
            CouponStatus from = CouponStatus.fromCode(fromCode);
            CouponStatus to = CouponStatus.fromCode(toCode);
            return from.canTransitionTo(to);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static CouponStatus mustBe(CouponStatus actual, CouponStatus expected) {
        if (actual != expected) {
            throw new IllegalStateException(
                String.format("状态校验失败: 期望 %s(%d), 实际 %s(%d)",
                    expected.getDesc(), expected.getCode(),
                    actual.getDesc(), actual.getCode()));
        }
        return actual;
    }
}
