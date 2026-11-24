package com.youhu.caregiver.catalog.dto;

import java.math.BigDecimal;

public record CaregiverView(
        Long id,
        String code,
        String name,
        String gender,
        Integer age,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal rating,
        Integer reviews,
        String qualification,
        String intro) {
}
