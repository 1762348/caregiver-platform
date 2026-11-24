package com.youhu.caregiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 优护 · 社区护工预约平台 后端入口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CaregiverPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaregiverPlatformApplication.class, args);
    }
}
