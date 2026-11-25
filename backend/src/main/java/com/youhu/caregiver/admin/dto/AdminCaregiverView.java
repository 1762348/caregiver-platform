package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.common.domain.Gender;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminCaregiverView(
        Long id,
        Long userId,
        String code,
        String name,
        Gender gender,
        LocalDate birth,
        String phone,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        RecordStatus status,
        BigDecimal rating,
        Integer reviews,
        LocalDateTime createdAt) {
}
