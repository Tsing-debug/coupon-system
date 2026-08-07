package com.ali.coupon.coupon.service;

import com.ali.coupon.coupon.entity.UserCoupon;
import com.ali.coupon.coupon.mapper.UserCouponMapper;
import com.ali.coupon.dto.SettlementCouponVO;
import com.ali.coupon.service.UserCouponQueryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Date;

@Service
public class UserCouponService extends ServiceImpl<UserCouponMapper, UserCoupon> {

    private final UserCouponQueryService queryService;

    public UserCouponService(UserCouponQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 发券成功后写 ZSet 索引 + detail 缓存
     */
    public void indexAfterIssue(UserCoupon coupon) {
        Date expireDate = Date.from(coupon.getValidEndTime()
                .atZone(ZoneId.systemDefault()).toInstant());
        queryService.addUserCouponIndex(coupon.getUserId(), coupon.getId(), expireDate);

        SettlementCouponVO vo = new SettlementCouponVO();
        vo.setCouponId(coupon.getId());
        vo.setTemplateId(coupon.getTemplateId());
        vo.setShopNumber(coupon.getShopNumber());
        vo.setCouponAmount(coupon.getCouponAmount());
        vo.setThresholdAmount(coupon.getThresholdAmount());
        vo.setStatus(coupon.getStatus());
        vo.setValidEndTime(coupon.getValidEndTime());
        queryService.cacheDetail(vo);
    }
}
