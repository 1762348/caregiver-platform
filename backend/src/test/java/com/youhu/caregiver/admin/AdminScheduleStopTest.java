package com.youhu.caregiver.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.admin.dto.StopScheduleResult;
import com.youhu.caregiver.admin.service.AdminScheduleService;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.dto.CreateOrderRequest;
import com.youhu.caregiver.order.dto.OrderView;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.domain.PaymentStatus;
import com.youhu.caregiver.payment.repository.PaymentRecordRepository;
import com.youhu.caregiver.receiver.domain.Receiver;
import com.youhu.caregiver.receiver.repository.ReceiverRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminScheduleStopTest {

    @Autowired
    private AdminScheduleService adminScheduleService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CaregiverScheduleRepository scheduleRepository;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Test
    void stopScheduleCancelsOrdersWithFullRefund() {
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(1L);
        schedule.setServiceDate(LocalDate.now().plusDays(3));
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setTotalQuota(1);
        schedule.setRemainingQuota(1);
        schedule.setPrice(new BigDecimal("200.00"));
        schedule.setStatus(ScheduleStatus.OPEN);
        Long scheduleId = scheduleRepository.saveAndFlush(schedule).getId();

        Receiver receiver = new Receiver();
        receiver.setUserId(3L);
        receiver.setName("停班测试");
        receiver.setIdCard("50010119900101999X");
        Long receiverId = receiverRepository.saveAndFlush(receiver).getId();

        OrderView order = orderService.create(3L, new CreateOrderRequest(scheduleId, receiverId));
        orderService.pay(3L, order.id(), PaymentChannel.MOCK);

        StopScheduleResult result = adminScheduleService.stop(scheduleId, "护工临时请假", 1L);

        assertThat(result.cancelledOrders()).isEqualTo(1);
        assertThat(result.refundTotal()).isEqualByComparingTo("200.00");
        assertThat(scheduleRepository.findById(scheduleId).orElseThrow().getStatus())
                .isEqualTo(ScheduleStatus.CLOSED);
        assertThat(orderRepository.findById(order.id()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.id()).get(0)
                .getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
}
