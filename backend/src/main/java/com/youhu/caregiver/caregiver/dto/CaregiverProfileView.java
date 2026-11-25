package com.youhu.caregiver.caregiver.dto;

import com.youhu.caregiver.common.domain.RecordStatus;
import java.math.BigDecimal;

public record CaregiverProfileView(
        Long id,
        String code,
        String name,
        String phone,
        String categoryName,
        BigDecimal price,
        BigDecimal rating,
        Integer reviews,
        RecordStatus status) {
}
