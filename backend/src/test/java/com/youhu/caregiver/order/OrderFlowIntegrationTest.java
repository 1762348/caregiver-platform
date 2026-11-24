package com.youhu.caregiver.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.message.service.MessageService;
import com.youhu.caregiver.order.domain.OrderStage;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.domain.OrderStatusLog;
import com.youhu.caregiver.order.domain.ProgressAction;
import com.youhu.caregiver.order.dto.CancelResult;
import com.youhu.caregiver.order.dto.CreateOrderRequest;
import com.youhu.caregiver.order.dto.OrderView;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.repository.OrderStatusLogRepository;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.domain.PaymentRecord;
import com.youhu.caregiver.payment.domain.PaymentStatus;
import com.youhu.caregiver.payment.repository.PaymentRecordRepository;
import com.youhu.caregiver.receiver.domain.Receiver;
import com.youhu.caregiver.receiver.repository.ReceiverRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderFlowIntegrationTest {

    private static final Long USER_ID = 3L;
    private static final Long CAREGIVER_USER_ID = 2L;
    private static final Long CAREGIVER_ID = 1L;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private CaregiverScheduleRepository scheduleRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderStatusLogRepository statusLogRepository;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    @Autowired
    private MessageService messageService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private int sequence = 0;

    private Receiver newReceiver() {
        Receiver receiver = new Receiver();
        receiver.setUserId(USER_ID);
        receiver.setName("测试被照顾人" + sequence);
        receiver.setIdCard(String.format("50010119900101%03dX", sequence++));
        return receiverRepository.save(receiver);
    }

    private CaregiverSchedule newSchedule(Long caregiverId, LocalDate date, int hour, int quota,
                                          String price) {
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(caregiverId);
        schedule.setServiceDate(date);
        schedule.setStartTime(LocalTime.of(hour, 0));
        schedule.setEndTime(LocalTime.of(hour + 4, 0));
        schedule.setTotalQuota(quota);
        schedule.setRemainingQuota(quota);
        schedule.setPrice(new BigDecimal(price));
        schedule.setStatus(ScheduleStatus.OPEN);
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Test
    void bookingPaymentProgressFlow() {
        Receiver receiver = newReceiver();
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(3), 8, 1, "200.00");

        OrderView order = orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiver.getId()));
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.stage()).isEqualTo(OrderStage.PENDING_PAYMENT);
        assertThat(order.amount()).isEqualByComparingTo("200.00");
        assertThat(order.canPay()).isTrue();
        assertThat(order.canCancel()).isTrue();
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isZero();

        OrderView paid = orderService.pay(USER_ID, order.id(), PaymentChannel.MOCK);
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.payTime()).isNotNull();

        OrderView onTheWay = orderService.updateProgress(CAREGIVER_USER_ID, order.id(), ProgressAction.DEPART);
        assertThat(onTheWay.stage()).isEqualTo(OrderStage.ON_THE_WAY);

        OrderView arrived = orderService.updateProgress(CAREGIVER_USER_ID, order.id(), ProgressAction.ARRIVE);
        assertThat(arrived.stage()).isEqualTo(OrderStage.ARRIVED);

        OrderView servicing = orderService.updateProgress(CAREGIVER_USER_ID, order.id(), ProgressAction.START);
        assertThat(servicing.status()).isEqualTo(OrderStatus.IN_SERVICE);
        assertThat(servicing.stage()).isEqualTo(OrderStage.IN_SERVICE);

        OrderView completed = orderService.updateProgress(CAREGIVER_USER_ID, order.id(), ProgressAction.COMPLETE);
        assertThat(completed.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(completed.stage()).isEqualTo(OrderStage.COMPLETED);

        List<OrderStatusLog> logs = statusLogRepository.findByOrderIdOrderByCreatedAtAsc(order.id());
        assertThat(logs).extracting(OrderStatusLog::getToStatus)
                .containsExactly(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID,
                        OrderStatus.IN_SERVICE, OrderStatus.COMPLETED);
        assertThat(messageService.unreadCount(USER_ID)).isGreaterThanOrEqualTo(4);
    }

    @Test
    void paidOrderCancelledWithFullRefund() {
        Receiver receiver = newReceiver();
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(3), 8, 1, "200.00");
        OrderView order = orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiver.getId()));
        orderService.pay(USER_ID, order.id(), PaymentChannel.MOCK);

        CancelResult result = orderService.cancel(USER_ID, order.id(), "临时有事");

        assertThat(result.order().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.refundAmount()).isEqualByComparingTo("200.00");
        assertThat(result.feeApplied()).isFalse();
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isEqualTo(1);

        PaymentRecord payment = paymentRecordRepository
                .findByOrderIdOrderByCreatedAtDesc(order.id()).get(0);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void unpaidOrderCancelReleasesQuotaWithoutRefund() {
        Receiver receiver = newReceiver();
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(3), 14, 1, "150.00");
        OrderView order = orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiver.getId()));

        CancelResult result = orderService.cancel(USER_ID, order.id(), null);

        assertThat(result.order().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.refundAmount()).isEqualByComparingTo("0");
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isEqualTo(1);
    }

    @Test
    void duplicateBookingForSameReceiverSlotIsRejected() {
        Receiver receiver = newReceiver();
        LocalDate date = LocalDate.now().plusDays(4);
        CaregiverSchedule first = newSchedule(CAREGIVER_ID, date, 8, 1, "200.00");
        CaregiverSchedule second = newSchedule(2L, date, 8, 1, "260.00");

        orderService.create(USER_ID, new CreateOrderRequest(first.getId(), receiver.getId()));

        assertThatThrownBy(() -> orderService.create(USER_ID,
                new CreateOrderRequest(second.getId(), receiver.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有预约");
        assertThat(scheduleRepository.findById(second.getId()).orElseThrow().getRemainingQuota())
                .isEqualTo(1);
    }

    @Test
    void soldOutScheduleCannotBeBooked() {
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(5), 8, 1, "200.00");
        orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), newReceiver().getId()));

        assertThatThrownBy(() -> orderService.create(USER_ID,
                new CreateOrderRequest(schedule.getId(), newReceiver().getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("约满");
    }

    @Test
    void caregiverCanOnlyUpdateOwnOrders() {
        Receiver receiver = newReceiver();
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(6), 8, 1, "200.00");
        OrderView order = orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiver.getId()));
        orderService.pay(USER_ID, order.id(), PaymentChannel.MOCK);

        assertThatThrownBy(() -> orderService.updateProgress(4L, order.id(), ProgressAction.DEPART))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作");
    }

    @Test
    void overdueUnpaidOrderIsCancelledAndQuotaReleased() {
        Receiver receiver = newReceiver();
        CaregiverSchedule schedule = newSchedule(CAREGIVER_ID, LocalDate.now().plusDays(7), 8, 1, "200.00");
        OrderView order = orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiver.getId()));

        jdbcTemplate.update("update orders set created_at = ? where id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(31)), order.id());
        entityManager.clear();

        int cancelled = orderService.expireOverdue();

        assertThat(cancelled).isGreaterThanOrEqualTo(1);
        assertThat(orderRepository.findById(order.id()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isEqualTo(1);
    }
}
