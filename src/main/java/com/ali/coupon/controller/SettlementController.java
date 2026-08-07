package com.ali.coupon.controller;

import com.ali.coupon.dto.SettlementCouponVO;
import com.ali.coupon.service.UserCouponQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    private final UserCouponQueryService queryService;

    public SettlementController(UserCouponQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/coupons")
    public Map<String, Object> getCoupons(@RequestParam Long userId) {
        List<SettlementCouponVO> coupons = queryService.queryForSettlement(userId);
        queryService.enrichWithCalculatedDiscount(coupons);
        return Map.of(
                "code", 200,
                "data", coupons,
                "count", coupons.size()
        );
    }
}
