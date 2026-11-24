package com.youhu.caregiver.payment.repository;

import com.youhu.caregiver.payment.domain.PaymentRecord;
import com.youhu.caregiver.payment.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByPaymentNo(String paymentNo);

    List<PaymentRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentRecord> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
            Long orderId, PaymentStatus status);
}
