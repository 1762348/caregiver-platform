package com.youhu.caregiver.payment.service;

import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.domain.PaymentRecord;
import java.math.BigDecimal;

/**
 * 支付网关抽象：当前提供 Mock 实现，接入微信/支付宝时新增实现即可。
 */
public interface PaymentGateway {

    PaymentResult charge(PaymentRecord record, PaymentChannel channel);

    boolean refund(PaymentRecord record, BigDecimal amount);

    /** 校验异步回调签名，防止伪造支付成功 */
    boolean verifyCallback(String paymentNo, String signature);
}
