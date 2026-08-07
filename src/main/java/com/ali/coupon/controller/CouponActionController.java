package com.ali.coupon.controller;

import com.ali.coupon.service.CouponLockService;
import com.ali.coupon.service.CouponUseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
public class CouponActionController {

    private final CouponLockService lockService;
    private final CouponUseService useService;

    public CouponActionController(CouponLockService lockService,
                                  CouponUseService useService) {
        this.lockService = lockService;
        this.useService = useService;
    }

    @PostMapping("/{couponId}/lock")
    public Map<String, Object> lock(@PathVariable Long couponId, @RequestParam Long userId) {
        boolean result = lockService.lockCoupon(userId, couponId);
        return Map.of("code", result ? 200 : 409, "success", result);
    }

    @PostMapping("/{couponId}/use")
    public Map<String, Object> use(@PathVariable Long couponId, @RequestParam Long userId) {
        boolean result = useService.useCoupon(userId, couponId);
        return Map.of("code", result ? 200 : 409, "success", result);
    }

    @PostMapping("/{couponId}/refund")
    public Map<String, Object> refund(@PathVariable Long couponId, @RequestParam Long userId) {
        boolean result = useService.refundCoupon(userId, couponId);
        return Map.of("code", result ? 200 : 409, "success", result);
    }
}
