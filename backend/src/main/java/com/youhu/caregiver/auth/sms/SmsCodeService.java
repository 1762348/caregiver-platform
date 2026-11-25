package com.youhu.caregiver.auth.sms;

import com.youhu.caregiver.common.exception.BusinessException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 验证码生命周期管理：发送频控、有效期、错误次数限制。
 *
 * <p>当前为单机内存实现，多实例部署时需替换为 Redis。</p>
 */
@Slf4j
@Service
public class SmsCodeService {

    private record CodeEntry(String code, Instant expiresAt, Instant nextSendAt, AtomicInteger attempts) {
    }

    private static final int CODE_BOUND = 1_000_000;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SmsSender smsSender;
    private final SmsProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public SmsCodeService(SmsSender smsSender, SmsProperties properties, Clock clock) {
        this.smsSender = smsSender;
        this.properties = properties;
        this.clock = clock;
    }

    public void send(String phone) {
        Instant now = clock.instant();
        CodeEntry previous = codes.get(phone);
        if (properties.resendIntervalSeconds() > 0 && previous != null
                && now.isBefore(previous.nextSendAt())) {
            long waitSeconds = Duration.between(now, previous.nextSendAt()).toSeconds() + 1;
            throw new BusinessException(429, "验证码发送过于频繁，请 " + waitSeconds + " 秒后重试");
        }

        String code = resolveCode();
        codes.put(phone, new CodeEntry(
                code,
                now.plusSeconds(properties.ttlSeconds()),
                now.plusSeconds(properties.resendIntervalSeconds()),
                new AtomicInteger()));
        smsSender.send(phone, code);
    }

    public void verify(String phone, String code) {
        Instant now = clock.instant();
        CodeEntry entry = codes.get(phone);
        if (entry == null || !now.isBefore(entry.expiresAt())) {
            codes.remove(phone);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (entry.attempts().incrementAndGet() > properties.maxAttempts()) {
            codes.remove(phone);
            throw new BusinessException(429, "验证码错误次数过多，请重新获取");
        }
        if (!entry.code().equals(code)) {
            throw new BusinessException(400, "验证码错误");
        }
        codes.remove(phone);
    }

    private String resolveCode() {
        String fixedCode = properties.fixedCode();
        if (fixedCode != null && !fixedCode.isBlank()) {
            return fixedCode;
        }
        return String.format("%06d", random.nextInt(CODE_BOUND));
    }
}
