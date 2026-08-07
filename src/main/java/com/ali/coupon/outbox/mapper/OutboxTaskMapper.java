package com.ali.coupon.outbox.mapper;

import com.ali.coupon.outbox.entity.OutboxTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OutboxTaskMapper extends BaseMapper<OutboxTask> {

    @Update("UPDATE outbox_task SET status = #{status}, update_time = NOW() WHERE business_key = #{businessKey}")
    int updateStatusByBusinessKey(String businessKey, Integer status);
}
