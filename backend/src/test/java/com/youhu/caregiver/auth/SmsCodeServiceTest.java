package com.youhu.caregiver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.auth.sms.SmsCodeService;
import com.youhu.caregiver.auth.sms.SmsProperties;
import com.youhu.caregiver.common.exception.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SmsCodeServiceTest {

    /** 可手动推进的时钟 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final MutableClock clock = new MutableClock();
    private final SmsProperties properties = new SmsProperties("mock", "123456", 300, 60, 3);
    private final SmsCodeService service = new SmsCodeService((phone, code) -> {
    }, properties, clock);

    @Test
    void fixedCodeCanBeVerifiedOnce() {
        service.send("13800000002");
        service.verify("13800000002", "123456");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.verify("13800000002", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void resendWithinIntervalIsRejected() {
        service.send("13800000002");

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.send("13800000002")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过于频繁");
    }

    @Test
    void codeExpiresAfterTtl() {
        service.send("13800000002");
        clock.advance(Duration.ofSeconds(301));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.verify("13800000002", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void tooManyWrongAttemptsInvalidatesCode() {
        service.send("13800000002");

        for (int i = 0; i < 3; i++) {
            assertThat(org.assertj.core.api.Assertions.catchThrowable(
                    () -> service.verify("13800000002", "000000")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("验证码错误");
        }

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> service.verify("13800000002", "123456")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("次数过多");
    }

    @Test
    void resendIsAllowedAfterInterval() {
        service.send("13800000002");
        clock.advance(Duration.ofSeconds(61));

        service.send("13800000002");
        service.verify("13800000002", "123456");
    }
}
