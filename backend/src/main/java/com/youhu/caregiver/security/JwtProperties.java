package com.youhu.caregiver.security;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。
 *
 * @param secret          HMAC 密钥，至少 32 字节
 * @param issuer          签发者
 * @param expiresMinutes  有效期（分钟），默认 7 天
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, long expiresMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret 至少需要 32 字节，请通过 JWT_SECRET 环境变量配置");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "caregiver-platform";
        }
        if (expiresMinutes <= 0) {
            expiresMinutes = 10080;
        }
    }
}
