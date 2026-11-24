package com.youhu.caregiver.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.order.domain.Order;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefundCalculatorTest {

    private final RefundCalculator calculator = new RefundCalculator();

    private Order order(String amount) {
        Order order = new Order();
        order.setAmount(new BigDecimal(amount));
        order.setServiceDate(LocalDate.of(2026, 6, 1));
        order.setServiceTime("10:00-12:00");
        return order;
    }

    @Test
    void fullRefundWhenMoreThanTwoHoursBeforeStart() {
        RefundResult result = calculator.calculate(order("200.00"),
                LocalDateTime.of(2026, 6, 1, 7, 0));

        assertThat(result.feeApplied()).isFalse();
        assertThat(result.refundAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void twentyPercentFeeWithinTwoHours() {
        RefundResult result = calculator.calculate(order("200.00"),
                LocalDateTime.of(2026, 6, 1, 8, 30));

        assertThat(result.feeApplied()).isTrue();
        assertThat(result.refundAmount()).isEqualByComparingTo("160.00");
    }

    @Test
    void exactlyTwoHoursIsCharged() {
        RefundResult result = calculator.calculate(order("100.00"),
                LocalDateTime.of(2026, 6, 1, 8, 0));

        assertThat(result.feeApplied()).isTrue();
        assertThat(result.refundAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void cannotCancelAfterServiceStarts() {
        assertThatThrownBy(() -> calculator.calculate(order("200.00"),
                LocalDateTime.of(2026, 6, 1, 10, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("服务已开始");
    }

    @Test
    void serviceStartIsParsedFromTimeRange() {
        assertThat(calculator.serviceStart(order("200.00")))
                .isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
    }
}
