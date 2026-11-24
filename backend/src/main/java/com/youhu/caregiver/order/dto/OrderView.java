package com.youhu.caregiver.order.dto;

import com.youhu.caregiver.order.domain.OrderStage;
import com.youhu.caregiver.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OrderView(
        Long id,
        String orderNo,
        OrderStatus status,
        OrderStage stage,
        BigDecimal amount,
        LocalDate serviceDate,
        String serviceTime,
        Long caregiverId,
        String caregiverName,
        String caregiverPhone,
        String categoryName,
        Long receiverId,
        String receiverName,
        LocalDateTime createdAt,
        LocalDateTime payTime,
        LocalDateTime cancelTime,
        LocalDateTime departedAt,
        LocalDateTime arrivedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String cancelReason,
        boolean canPay,
        boolean canCancel) {
}
