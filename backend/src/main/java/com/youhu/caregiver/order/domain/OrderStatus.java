package com.youhu.caregiver.order.domain;

import java.util.Set;

/**
 * 订单状态机。
 *
 * <pre>
 * PENDING_PAYMENT → PAID → IN_SERVICE → COMPLETED
 *        ↘ CANCELLED
 * </pre>
 * 仅允许上图中的正向流转，其它组合一律拒绝（P3 实现校验）。
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    IN_SERVICE,
    COMPLETED,
    CANCELLED;

    private static final Set<OrderStatus> CANCELLABLE = Set.of(PENDING_PAYMENT, PAID);

    public boolean canTransitTo(OrderStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == IN_SERVICE || target == CANCELLED;
            case IN_SERVICE -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    public boolean isCancellable() {
        return CANCELLABLE.contains(this);
    }
}
