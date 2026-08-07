package com.ali.coupon.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 锁券并发测试：50线程抢1张券，仅1条成功
 */
@SpringBootTest(properties = "spring.profiles.active=test")
class CouponLockServiceTest {

    @Autowired
    private CouponLockService lockService;

    @Test
    void shouldOnlyOneThreadLockSuccessfully() throws InterruptedException {
        final Long userId = 50001L;
        final Long couponId = 99999L;
        final int threadCount = 50;

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    boolean result = lockService.lockCoupon(userId, couponId);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 状态非法等异常忽略
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        assertEquals(1, successCount.get(), "50线程并发锁券，仅有1条应成功");
    }
}
