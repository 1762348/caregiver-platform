package com.youhu.caregiver.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 文档配置：Swagger UI 见 /swagger-ui.html */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI caregiverOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("优护 · 社区护工预约平台 API")
                        .version("0.1.0")
                        .description("""
                                手机号验证码登录后，将返回的 token 填入 Swagger 的 Authorize 即可调试。
                                管理端接口需要 ADMIN 角色，护工端接口需要 CAREGIVER 角色。
                                """))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
