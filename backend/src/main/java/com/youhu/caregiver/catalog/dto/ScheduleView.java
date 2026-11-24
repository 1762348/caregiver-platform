package com.youhu.caregiver.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleView(
        Long id,
        Long caregiverId,
        String caregiverName,
        Long categoryId,
        String categoryName,
        LocalDate serviceDate,
        String startTime,
        String endTime,
        String timeRange,
        int remainingQuota,
        int totalQuota,
        BigDecimal price) {
}
