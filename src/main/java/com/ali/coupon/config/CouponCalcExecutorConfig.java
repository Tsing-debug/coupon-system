package com.ali.coupon.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class CouponCalcExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(CouponCalcExecutorConfig.class);

    @Bean("couponCalcExecutor")
    public Executor couponCalcExecutor() {
        return new ThreadPoolExecutor(
                10,                                     // 核心线程数
                50,                                     // 最大线程数
                60L, TimeUnit.SECONDS,                   // 空闲线程存活时间
                new LinkedBlockingQueue<>(200),           // 队列容量
                new ThreadPoolExecutor.CallerRunsPolicy(),// 拒绝策略：调用者线程执行
                r -> {
                    Thread t = new Thread(r);
                    t.setName("coupon-calc-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );
    }
}
