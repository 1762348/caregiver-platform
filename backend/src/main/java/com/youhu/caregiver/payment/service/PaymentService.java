package com.youhu.caregiver.payment.service;

import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.order.domain.Order;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.domain.OrderStatusLog;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.repository.OrderStatusLogRepository;
import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.domain.PaymentRecord;
import com.youhu.caregiver.payment.domain.PaymentStatus;
import com.youhu.caregiver.payment.repository.PaymentRecordRepository;
import com.youhu.caregiver.user.domain.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 支付流水与回调处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentGateway paymentGateway;
    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final Clock clock;

    @Transactional
    public PaymentResult pay(Order order, PaymentChannel channel) {
        LocalDateTime now = LocalDateTime.now(clock);
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(nextPaymentNo());
        record.setOrderId(order.getId());
        record.setChannel(channel);
        record.setAmount(order.getAmount());
        record.setStatus(PaymentStatus.PENDING);
        paymentRecordRepository.save(record);

        PaymentResult result = paymentGateway.charge(record, channel);
        record.setStatus(result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (result.success()) {
            record.setPaidAt(now);
        }
        record.setRawPayload(result.message());
        paymentRecordRepository.save(record);
        return result;
    }

    @Transactional
    public BigDecimal refund(Order order, BigDecimal amount) {
        PaymentRecord record = paymentRecordRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCESS)
                .orElse(null);
        if (record == null) {
            return BigDecimal.ZERO;
        }
        if (!paymentGateway.refund(record, amount)) {
            throw new BusinessException(500, "退款失败，请联系客服处理");
        }
        record.setStatus(PaymentStatus.REFUNDED);
        record.setRawPayload("退款金额 " + amount.toPlainString());
        paymentRecordRepository.save(record);
        return amount;
    }

    /**
     * 异步支付回调：签名校验 + 幂等。
     * Mock 支付在同步接口中已完成，这里保留完整链路，便于替换真实网关。
     */
    @Transactional
    public void handleCallback(String paymentNo, boolean success, String signature) {
        if (!paymentGateway.verifyCallback(paymentNo, signature)) {
            throw new BusinessException(403, "支付回调签名校验失败");
        }
        PaymentRecord record = paymentRecordRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> BusinessException.notFound("支付单不存在"));
        if (record.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        record.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (success) {
            record.setPaidAt(now);
        }
        record.setRawPayload("async callback");
        paymentRecordRepository.save(record);

        if (!success) {
            return;
        }
        Order order = orderRepository.findById(record.getOrderId()).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAID);
            order.setPayTime(now);
            orderRepository.save(order);

            OrderStatusLog statusLog = new OrderStatusLog();
            statusLog.setOrderId(order.getId());
            statusLog.setFromStatus(OrderStatus.PENDING_PAYMENT);
            statusLog.setToStatus(OrderStatus.PAID);
            statusLog.setOperatorRole(UserRole.USER);
            statusLog.setRemark("支付回调");
            orderStatusLogRepository.save(statusLog);
        }
        log.info("支付回调处理完成，paymentNo={}，orderId={}", paymentNo, record.getOrderId());
    }

    private String nextPaymentNo() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}
