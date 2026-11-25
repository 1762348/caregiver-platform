package com.youhu.caregiver.order.repository;

import com.youhu.caregiver.order.domain.Order;
import com.youhu.caregiver.order.domain.OrderStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId, OrderStatus status, Pageable pageable);

    Page<Order> findByCaregiverIdOrderByCreatedAtDesc(Long caregiverId, Pageable pageable);

    long countByUserIdAndStatus(Long userId, OrderStatus status);

    /** 同一被照顾人同一天同一时段只允许存在一个有效订单 */
    boolean existsByReceiverIdAndServiceDateAndServiceTimeAndStatusIn(
            Long receiverId, LocalDate serviceDate, String serviceTime, Collection<OrderStatus> statuses);

    /** 支付超时扫描：创建时间早于 deadline 的待支付订单 */
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime deadline);

    boolean existsByReceiverId(Long receiverId);

    long countByScheduleId(Long scheduleId);

    List<Order> findByScheduleIdAndStatusIn(Long scheduleId, Collection<OrderStatus> statuses);
}
