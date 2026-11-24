package com.youhu.caregiver.order.domain;

import com.youhu.caregiver.common.domain.CreatedEntity;
import com.youhu.caregiver.user.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 订单状态流转日志（审计用，只增不改） */
@Entity
@Table(name = "order_status_logs")
@Getter
@Setter
@NoArgsConstructor
public class OrderStatusLog extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private OrderStatus toStatus;

    @Column(name = "operator_id")
    private Long operatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator_role", length = 20)
    private UserRole operatorRole;

    @Column(length = 255)
    private String remark;
}
