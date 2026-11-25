package com.youhu.caregiver.caregiver.dto;

import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CaregiverScheduleView(
        Long id,
        LocalDate serviceDate,
        String timeRange,
        int totalQuota,
        int remainingQuota,
        BigDecimal price,
        ScheduleStatus status) {
}
