package com.youhu.caregiver.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCallbackRequest(
        @NotBlank String paymentNo,
        boolean success,
        @NotBlank String signature) {
}
