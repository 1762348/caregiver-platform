package com.youhu.caregiver.order.job;

import com.youhu.caregiver.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时取消超时未支付订单并释放名额 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void cancelExpiredOrders() {
        int cancelled = orderService.expireOverdue();
        if (cancelled > 0) {
            log.info("自动取消超时未支付订单 {} 笔", cancelled);
        }
    }
}
