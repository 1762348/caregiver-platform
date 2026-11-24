package com.youhu.caregiver.order.service;

import java.math.BigDecimal;

public record RefundResult(BigDecimal refundAmount, boolean feeApplied) {
}
