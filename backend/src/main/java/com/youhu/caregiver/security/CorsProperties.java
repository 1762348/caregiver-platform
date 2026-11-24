package com.youhu.caregiver.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 跨域配置：前后端分离部署时允许的来源 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173");
        }
    }
}
