package com.youhu.caregiver.caregiver.repository;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaregiverRepository extends JpaRepository<Caregiver, Long> {

    Optional<Caregiver> findByUserId(Long userId);

    Optional<Caregiver> findByCode(String code);

    boolean existsByCode(String code);

    List<Caregiver> findByStatus(RecordStatus status);

    List<Caregiver> findByCategoryIdAndStatus(Long categoryId, RecordStatus status);

    Page<Caregiver> findByStatus(RecordStatus status, Pageable pageable);
}
