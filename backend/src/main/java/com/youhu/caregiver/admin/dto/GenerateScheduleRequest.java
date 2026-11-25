package com.youhu.caregiver.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateScheduleRequest(
        @NotNull(message = "请选择护工") Long caregiverId,
        @NotNull(message = "请选择开始日期") LocalDate from,
        @NotNull(message = "请选择结束日期") LocalDate to,
        @NotNull(message = "请设置名额")
        @Min(value = 1, message = "名额至少为 1")
        Integer totalQuota) {
}
