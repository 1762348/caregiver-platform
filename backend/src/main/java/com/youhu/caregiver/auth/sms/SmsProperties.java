package com.youhu.caregiver.auth.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信验证码配置。
 *
 * @param provider              实现方：mock / aliyun（P7 扩展）
 * @param fixedCode             固定验证码，仅开发/测试使用；为空则随机生成
 * @param ttlSeconds            验证码有效期
 * @param resendIntervalSeconds 同一手机号重发间隔
 * @param maxAttempts           最大校验次数
 */
@ConfigurationProperties(prefix = "app.sms")
public record SmsProperties(
        String provider,
        String fixedCode,
        long ttlSeconds,
        long resendIntervalSeconds,
        int maxAttempts) {

    public SmsProperties {
        if (provider == null || provider.isBlank()) {
            provider = "mock";
        }
        if (ttlSeconds <= 0) {
            ttlSeconds = 300;
        }
        if (resendIntervalSeconds <= 0) {
            resendIntervalSeconds = 60;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 5;
        }
    }
}
