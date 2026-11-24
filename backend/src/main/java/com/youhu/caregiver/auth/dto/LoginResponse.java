package com.youhu.caregiver.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserProfile user) {
}
