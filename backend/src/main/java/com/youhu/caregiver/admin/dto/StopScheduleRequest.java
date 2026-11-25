package com.youhu.caregiver.admin.dto;

import jakarta.validation.constraints.Size;

public record StopScheduleRequest(@Size(max = 255, message = "停班原因过长") String reason) {
}
