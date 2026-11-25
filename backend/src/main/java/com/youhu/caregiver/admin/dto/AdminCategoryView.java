package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.common.domain.RecordStatus;
import java.math.BigDecimal;

public record AdminCategoryView(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal basePrice,
        RecordStatus status,
        long caregiverCount) {
}
