package com.youhu.caregiver.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /** 统一时钟，便于测试中注入固定时间 */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
