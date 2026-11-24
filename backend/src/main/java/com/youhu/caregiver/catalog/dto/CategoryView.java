package com.youhu.caregiver.catalog.dto;

import java.math.BigDecimal;

public record CategoryView(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal basePrice) {
}
