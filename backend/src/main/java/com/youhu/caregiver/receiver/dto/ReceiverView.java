package com.youhu.caregiver.receiver.dto;

import java.time.LocalDate;

public record ReceiverView(
        Long id,
        String name,
        String idCard,
        String phone,
        String relation,
        String health,
        String special,
        String gender,
        LocalDate birth) {
}
