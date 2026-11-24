package com.youhu.caregiver.order.repository;

import com.youhu.caregiver.order.domain.OrderStatusLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {

    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
