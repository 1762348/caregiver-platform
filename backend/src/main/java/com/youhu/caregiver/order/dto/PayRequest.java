package com.youhu.caregiver.order.dto;

import com.youhu.caregiver.payment.domain.PaymentChannel;
import jakarta.validation.constraints.NotNull;

public record PayRequest(@NotNull(message = "请选择支付方式") PaymentChannel channel) {
}
