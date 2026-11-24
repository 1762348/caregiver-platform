package com.youhu.caregiver.order.dto;

import java.math.BigDecimal;

public record CancelResult(OrderView order, BigDecimal refundAmount, boolean feeApplied) {
}
