package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull(message = "请选择护工") Long caregiverId,
        @NotNull(message = "请选择日期") LocalDate serviceDate,
        @NotNull(message = "请选择开始时间") LocalTime startTime,
        @NotNull(message = "请选择结束时间") LocalTime endTime,
        @NotNull(message = "请设置名额")
        @Min(value = 1, message = "名额至少为 1")
        Integer totalQuota,
        @NotNull(message = "请设置价格")
        @DecimalMin(value = "0", message = "价格不能为负")
        BigDecimal price,
        ScheduleStatus status) {
}
