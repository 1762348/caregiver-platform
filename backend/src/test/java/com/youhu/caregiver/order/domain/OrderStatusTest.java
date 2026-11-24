package com.youhu.caregiver.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void pendingPaymentCanBePaidOrCancelled() {
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitTo(OrderStatus.PAID)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.canTransitTo(OrderStatus.COMPLETED)).isFalse();
        assertThat(OrderStatus.PENDING_PAYMENT.isCancellable()).isTrue();
    }

    @Test
    void paidCanStartServiceOrCancel() {
        assertThat(OrderStatus.PAID.canTransitTo(OrderStatus.IN_SERVICE)).isTrue();
        assertThat(OrderStatus.PAID.canTransitTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.canTransitTo(OrderStatus.COMPLETED)).isFalse();
    }

    @Test
    void inServiceCanOnlyComplete() {
        assertThat(OrderStatus.IN_SERVICE.canTransitTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.IN_SERVICE.canTransitTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void terminalStatesAreFinal() {
        assertThat(OrderStatus.COMPLETED.canTransitTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStatus.CANCELLED.canTransitTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.COMPLETED.isCancellable()).isFalse();
        assertThat(OrderStatus.CANCELLED.isCancellable()).isFalse();
    }
}
