package com.ali.coupon.coupon;

import com.ali.coupon.common.enums.CouponStatus;
import com.ali.coupon.common.guard.StateMachineGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateMachineGuardTest {

    // ========== 合法流转路径 (6条) ==========

    @Test
    void shouldAllowPendingToLocked() {
        assertDoesNotThrow(() -> StateMachineGuard.validateTransition(CouponStatus.PENDING, CouponStatus.LOCKED));
        assertTrue(CouponStatus.PENDING.canTransitionTo(CouponStatus.LOCKED));
    }

    @Test
    void shouldAllowPendingToExpired() {
        assertDoesNotThrow(() -> StateMachineGuard.validateTransition(CouponStatus.PENDING, CouponStatus.EXPIRED));
        assertTrue(CouponStatus.PENDING.canTransitionTo(CouponStatus.EXPIRED));
    }

    @Test
    void shouldAllowLockedToUsed() {
        assertDoesNotThrow(() -> StateMachineGuard.validateTransition(CouponStatus.LOCKED, CouponStatus.USED));
        assertTrue(CouponStatus.LOCKED.canTransitionTo(CouponStatus.USED));
    }

    @Test
    void shouldAllowLockedToRefunded() {
        assertDoesNotThrow(() -> StateMachineGuard.validateTransition(CouponStatus.LOCKED, CouponStatus.REFUNDED));
        assertTrue(CouponStatus.LOCKED.canTransitionTo(CouponStatus.REFUNDED));
    }

    @Test
    void shouldAllowLockedToPending() {
        assertDoesNotThrow(() -> StateMachineGuard.validateTransition(CouponStatus.LOCKED, CouponStatus.PENDING));
        assertTrue(CouponStatus.LOCKED.canTransitionTo(CouponStatus.PENDING));
    }

    @Test
    void shouldAllowMustBeWhenStatusMatches() {
        assertEquals(CouponStatus.PENDING, StateMachineGuard.mustBe(CouponStatus.PENDING, CouponStatus.PENDING));
    }

    // ========== 非法流转路径 (3条) ==========

    @Test
    void shouldRejectExpiredToUsed() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> StateMachineGuard.validateTransition(CouponStatus.EXPIRED, CouponStatus.USED));
        assertTrue(ex.getMessage().contains("不允许的状态流转"));
    }

    @Test
    void shouldRejectUsedToPending() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> StateMachineGuard.validateTransition(CouponStatus.USED, CouponStatus.PENDING));
        assertTrue(ex.getMessage().contains("不允许的状态流转"));
    }

    @Test
    void shouldRejectRefundedToUsed() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> StateMachineGuard.validateTransition(CouponStatus.REFUNDED, CouponStatus.USED));
        assertTrue(ex.getMessage().contains("不允许的状态流转"));
    }

    // ========== 边界测试 ==========

    @Test
    void shouldRejectNullSource() {
        assertThrows(IllegalArgumentException.class,
            () -> StateMachineGuard.validateTransition(null, CouponStatus.USED));
    }

    @Test
    void shouldRejectNullTarget() {
        assertThrows(IllegalArgumentException.class,
            () -> StateMachineGuard.validateTransition(CouponStatus.PENDING, null));
    }

    @Test
    void shouldConvertCodeCorrectly() {
        assertEquals(CouponStatus.PENDING, CouponStatus.fromCode(1));
        assertEquals(CouponStatus.LOCKED, CouponStatus.fromCode(2));
        assertEquals(CouponStatus.USED, CouponStatus.fromCode(3));
        assertEquals(CouponStatus.REFUNDED, CouponStatus.fromCode(4));
        assertEquals(CouponStatus.EXPIRED, CouponStatus.fromCode(5));
    }

    @Test
    void shouldRejectInvalidCode() {
        assertThrows(IllegalArgumentException.class, () -> CouponStatus.fromCode(99));
    }

    @Test
    void terminalStatesHaveNoTransitions() {
        assertTrue(CouponStatus.USED.getAllowedNextStatus().isEmpty());
        assertTrue(CouponStatus.REFUNDED.getAllowedNextStatus().isEmpty());
        assertTrue(CouponStatus.EXPIRED.getAllowedNextStatus().isEmpty());
    }
}
