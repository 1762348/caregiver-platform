package com.youhu.caregiver.caregiver.repository;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaregiverRepository extends JpaRepository<Caregiver, Long> {

    Optional<Caregiver> findByUserId(Long userId);

    Optional<Caregiver> findByCode(String code);

    boolean existsByCode(String code);

    List<Caregiver> findByStatus(RecordStatus status);

    List<Caregiver> findByCategoryIdAndStatus(Long categoryId, RecordStatus status);

    Page<Caregiver> findByStatus(RecordStatus status, Pageable pageable);

    Page<Caregiver> findByCategoryIdAndStatus(Long categoryId, RecordStatus status, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    @Query("""
            select c from Caregiver c
             where (:keyword is null or :keyword = ''
                    or lower(c.name) like lower(concat('%', :keyword, '%'))
                    or lower(c.code) like lower(concat('%', :keyword, '%')))
               and (:status is null or c.status = :status)
            """)
    Page<Caregiver> search(@Param("keyword") String keyword,
                           @Param("status") RecordStatus status,
                           Pageable pageable);
}
