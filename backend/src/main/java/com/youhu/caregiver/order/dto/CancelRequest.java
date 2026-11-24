package com.youhu.caregiver.order.dto;

import jakarta.validation.constraints.Size;

public record CancelRequest(@Size(max = 255, message = "取消原因过长") String reason) {
}
