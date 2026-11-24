package com.youhu.caregiver.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发环境短信实现：不真正发送，只把验证码写日志，便于本地联调与演示。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String phone, String code) {
        log.info("【Mock 短信】手机号 {} 的验证码为 {}，5 分钟内有效", phone, code);
    }
}
