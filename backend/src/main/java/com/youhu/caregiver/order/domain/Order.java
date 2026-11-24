package com.youhu.caregiver.order.domain;

import com.youhu.caregiver.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 服务订单 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32, unique = true)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "caregiver_id", nullable = false)
    private Long caregiverId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "service_time", nullable = false, length = 20)
    private String serviceTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    /** 护工出发时间 */
    @Column(name = "departed_at")
    private LocalDateTime departedAt;

    /** 护工到达时间 */
    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    /** 开始服务时间 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 服务完成时间 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 取消原因 */
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}
