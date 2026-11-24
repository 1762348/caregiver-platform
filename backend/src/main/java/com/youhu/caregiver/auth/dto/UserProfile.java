package com.youhu.caregiver.auth.dto;

import java.util.List;

/** 当前登录用户信息（不含敏感字段） */
public record UserProfile(
        Long id,
        String phone,
        String name,
        String role,
        String avatar,
        List<String> permissions) {
}
