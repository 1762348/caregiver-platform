package com.youhu.caregiver.order.service;

import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.order.domain.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

/**
 * 取消退款规则：
 * <ul>
 *   <li>服务开始前 2 小时以上：全额退款</li>
 *   <li>服务开始前 2 小时内：收取 20% 手续费</li>
 *   <li>服务已开始：不可取消</li>
 * </ul>
 */
@Component
public class RefundCalculator {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.20");
    private static final long FREE_CANCEL_MINUTES = 2 * 60;

    public LocalDateTime serviceStart(Order order) {
        String timeRange = order.getServiceTime();
        if (timeRange == null || !timeRange.contains("-")) {
            throw new IllegalStateException("订单服务时段格式不正确：" + timeRange);
        }
        String start = timeRange.substring(0, timeRange.indexOf('-')).trim();
        return LocalDateTime.of(order.getServiceDate(), LocalTime.parse(start));
    }

    public RefundResult calculate(Order order, LocalDateTime now) {
        LocalDateTime start = serviceStart(order);
        if (!now.isBefore(start)) {
            throw new BusinessException(400, "服务已开始，无法取消");
        }
        long minutesUntilStart = Duration.between(now, start).toMinutes();
        boolean feeApplied = minutesUntilStart <= FREE_CANCEL_MINUTES;
        BigDecimal refund = feeApplied
                ? order.getAmount().multiply(BigDecimal.ONE.subtract(FEE_RATE))
                        .setScale(2, RoundingMode.HALF_UP)
                : order.getAmount();
        return new RefundResult(refund, feeApplied);
    }
}
