package com.youhu.caregiver.message.dto;

import com.youhu.caregiver.message.domain.MessageType;
import java.time.LocalDateTime;

public record MessageView(
        Long id,
        String title,
        String body,
        MessageType type,
        String deeplink,
        boolean read,
        LocalDateTime createdAt) {
}
