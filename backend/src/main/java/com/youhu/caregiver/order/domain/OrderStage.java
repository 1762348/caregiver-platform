package com.youhu.caregiver.order.domain;

/** 面向用户的订单进度阶段（由订单时间字段推导） */
public enum OrderStage {
    PENDING_PAYMENT,
    PENDING_DEPARTURE,
    ON_THE_WAY,
    ARRIVED,
    IN_SERVICE,
    COMPLETED,
    CANCELLED;

    public static OrderStage from(Order order) {
        return switch (order.getStatus()) {
            case PENDING_PAYMENT -> PENDING_PAYMENT;
            case CANCELLED -> CANCELLED;
            case COMPLETED -> COMPLETED;
            case IN_SERVICE -> IN_SERVICE;
            case PAID -> {
                if (order.getArrivedAt() != null) {
                    yield ARRIVED;
                }
                if (order.getDepartedAt() != null) {
                    yield ON_THE_WAY;
                }
                yield PENDING_DEPARTURE;
            }
        };
    }
}
