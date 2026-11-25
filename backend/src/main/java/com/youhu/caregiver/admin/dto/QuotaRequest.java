package com.youhu.caregiver.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuotaRequest(@NotNull @Min(value = 0, message = "名额不能为负") Integer remainingQuota) {
}
