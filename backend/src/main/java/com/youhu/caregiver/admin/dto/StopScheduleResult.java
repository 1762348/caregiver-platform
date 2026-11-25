package com.youhu.caregiver.admin.dto;

import java.math.BigDecimal;

public record StopScheduleResult(
        Long scheduleId,
        int cancelledOrders,
        BigDecimal refundTotal) {
}
