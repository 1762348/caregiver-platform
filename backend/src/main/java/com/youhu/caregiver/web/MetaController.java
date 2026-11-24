package com.youhu.caregiver.web;

import com.youhu.caregiver.common.api.ApiResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 元信息接口：用于确认服务已启动、当前环境与版本。
 */
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final Environment environment;
    private final String applicationName;

    public MetaController(Environment environment,
                          @Value("${spring.application.name:caregiver-platform}") String applicationName) {
        this.environment = environment;
        this.applicationName = applicationName;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> meta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", applicationName);
        body.put("displayName", "优护 · 社区护工预约平台");
        body.put("version", "0.1.0");
        body.put("profile", String.join(",", environment.getActiveProfiles()));
        body.put("serverTime", OffsetDateTime.now().toString());
        return ApiResponse.ok(body);
    }
}
