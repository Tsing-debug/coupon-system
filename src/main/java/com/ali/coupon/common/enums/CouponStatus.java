package com.ali.coupon.common.enums;

import java.util.Set;

public enum CouponStatus {
    PENDING(1, "待使用", Set.of(2, 5)),
    LOCKED(2, "锁券中", Set.of(3, 4, 1)),
    USED(3, "已核销", Set.of()),
    REFUNDED(4, "已退款", Set.of()),
    EXPIRED(5, "已过期", Set.of());

    private final int code;
    private final String desc;
    private final Set<Integer> allowedNextStatus;

    CouponStatus(int code, String desc, Set<Integer> allowedNextStatus) {
        this.code = code;
        this.desc = desc;
        this.allowedNextStatus = allowedNextStatus;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Set<Integer> getAllowedNextStatus() {
        return allowedNextStatus;
    }

    public boolean canTransitionTo(CouponStatus target) {
        return allowedNextStatus.contains(target.code);
    }

    public void transitionTo(CouponStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("不允许的状态流转: %s(%d) -> %s(%d)", this.desc, this.code, target.desc, target.code));
        }
    }

    public static CouponStatus fromCode(int code) {
        for (CouponStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的券状态码: " + code);
    }
}
