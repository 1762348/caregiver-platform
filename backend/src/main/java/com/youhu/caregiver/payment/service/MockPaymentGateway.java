package com.youhu.caregiver.payment.service;

import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.domain.PaymentRecord;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mock 支付网关：立即返回成功，回调签名使用 HMAC-SHA256，仅用于开发与演示。
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String ALGORITHM = "HmacSHA256";

    private final String secret;

    public MockPaymentGateway(@Value("${app.payment.mock-secret:mock-payment-secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public PaymentResult charge(PaymentRecord record, PaymentChannel channel) {
        log.info("【Mock 支付】订单 {} 通过 {} 支付 {} 元", record.getOrderId(), channel,
                record.getAmount());
        return new PaymentResult(true, record.getPaymentNo(), "模拟支付成功");
    }

    @Override
    public boolean refund(PaymentRecord record, BigDecimal amount) {
        log.info("【Mock 退款】支付单 {} 退款 {} 元", record.getPaymentNo(), amount);
        return true;
    }

    @Override
    public boolean verifyCallback(String paymentNo, String signature) {
        return signature != null && sign(paymentNo).equals(signature);
    }

    /** 供联调/测试生成回调签名 */
    public String sign(String paymentNo) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(mac.doFinal(paymentNo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("回调签名失败", ex);
        }
    }
}
