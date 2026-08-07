package com.ali.coupon.coupon.mapper;

import com.ali.coupon.coupon.entity.UserCoupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Insert("<script>" +
            "<foreach collection='list' item='item' separator=';'>" +
            "INSERT IGNORE INTO user_coupon " +
            "(user_id, template_id, shop_number, batch_no, status, coupon_amount, valid_start_time, valid_end_time, create_time, update_time) " +
            "VALUES (" +
            "#{item.userId}, #{item.templateId}, #{item.shopNumber}, #{item.batchNo}, " +
            "#{item.status}, #{item.couponAmount}, #{item.validStartTime}, #{item.validEndTime}, NOW(), NOW()" +
            ")" +
            "</foreach>" +
            "</script>")
    int batchInsertIgnore(@Param("list") List<UserCoupon> list);

    /**
     * 锁券 CAS 更新：status + version 双重校验
     * status=1(待使用) → status=2(锁券中)
     */
    @Update("UPDATE user_coupon SET status = #{newStatus}, lock_time = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = #{oldStatus} AND version = #{version}")
    int lockCouponById(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("newStatus") Integer newStatus, @Param("oldStatus") Integer oldStatus,
                       @Param("version") Integer version);

    /**
     * 核销：status=2(锁券中) → status=3(已核销)
     */
    @Update("UPDATE user_coupon SET status = #{newStatus}, use_time = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = #{oldStatus}")
    int useCouponById(@Param("id") Long id, @Param("userId") Long userId,
                      @Param("newStatus") Integer newStatus, @Param("oldStatus") Integer oldStatus);

    /**
     * 退款：status IN (1,2) → status=4(已退款)
     */
    @Update("UPDATE user_coupon SET status = 4, refund_time = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND user_id = #{userId} AND status IN (1, 2)")
    int refundCouponById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 通用状态更新（带 oldStatus 条件，防并发冲突）
     */
    @Update("UPDATE user_coupon SET status = #{newStatus}, version = version + 1 " +
            "WHERE id = #{id} AND status = #{oldStatus}")
    int updateStatus(@Param("id") Long id,
                     @Param("newStatus") Integer newStatus,
                     @Param("oldStatus") Integer oldStatus);
}
