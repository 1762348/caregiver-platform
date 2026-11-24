package com.youhu.caregiver.payment.service;

public record PaymentResult(boolean success, String paymentNo, String message) {
}
