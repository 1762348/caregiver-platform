package com.youhu.caregiver.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "请选择排班") Long scheduleId,
        @NotNull(message = "请选择被照顾人") Long receiverId) {
}
