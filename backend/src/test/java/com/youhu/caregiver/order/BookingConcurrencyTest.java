package com.youhu.caregiver.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.order.dto.CreateOrderRequest;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.receiver.domain.Receiver;
import com.youhu.caregiver.receiver.repository.ReceiverRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 防超卖：剩余 1 个名额时并发下单，必须只有 1 单成功。
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {

    private static final Long USER_ID = 3L;
    private static final int ATTEMPTS = 6;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ReceiverRepository receiverRepository;
    @Autowired
    private CaregiverScheduleRepository scheduleRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void onlyOneBookingWinsForLastQuota() throws Exception {
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(1L);
        schedule.setServiceDate(LocalDate.now().plusDays(10));
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setTotalQuota(1);
        schedule.setRemainingQuota(1);
        schedule.setPrice(new BigDecimal("200.00"));
        schedule.setStatus(ScheduleStatus.OPEN);
        scheduleRepository.saveAndFlush(schedule);

        List<Long> receiverIds = new ArrayList<>();
        for (int i = 0; i < ATTEMPTS; i++) {
            Receiver receiver = new Receiver();
            receiver.setUserId(USER_ID);
            receiver.setName("并发测试" + i);
            receiver.setIdCard(String.format("50010119910101%03dX", i));
            receiverIds.add(receiverRepository.saveAndFlush(receiver).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);
        CountDownLatch ready = new CountDownLatch(ATTEMPTS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < ATTEMPTS; i++) {
            Long receiverId = receiverIds.get(i);
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    transactionTemplate.execute(status ->
                            orderService.create(USER_ID, new CreateOrderRequest(schedule.getId(), receiverId)));
                    success.incrementAndGet();
                } catch (BusinessException ex) {
                    conflict.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<?> future : futures) {
            future.get(20, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(ATTEMPTS - 1);
        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isZero();
        assertThat(orderRepository.countByScheduleId(schedule.getId())).isEqualTo(1);
    }
}
