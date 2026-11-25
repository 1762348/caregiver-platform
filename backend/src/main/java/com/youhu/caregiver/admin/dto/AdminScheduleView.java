package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import java.math.BigDecimal;

public record AdminScheduleView(
        Long id,
        Long caregiverId,
        String caregiverName,
        java.time.LocalDate serviceDate,
        String timeRange,
        int totalQuota,
        int remainingQuota,
        BigDecimal price,
        ScheduleStatus status) {
}
