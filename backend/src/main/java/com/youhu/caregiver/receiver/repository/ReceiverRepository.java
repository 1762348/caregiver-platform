package com.youhu.caregiver.receiver.repository;

import com.youhu.caregiver.receiver.domain.Receiver;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiverRepository extends JpaRepository<Receiver, Long> {

    List<Receiver> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Receiver> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndIdCard(Long userId, String idCard);

    long countByUserId(Long userId);
}
