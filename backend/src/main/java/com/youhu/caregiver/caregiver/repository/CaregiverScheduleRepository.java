package com.youhu.caregiver.caregiver.repository;

import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaregiverScheduleRepository extends JpaRepository<CaregiverSchedule, Long> {

    List<CaregiverSchedule> findByServiceDateAndStatusOrderByStartTimeAsc(
            LocalDate serviceDate, ScheduleStatus status);

    List<CaregiverSchedule> findByCaregiverIdAndServiceDateBetweenOrderByServiceDateAscStartTimeAsc(
            Long caregiverId, LocalDate from, LocalDate to);

    List<CaregiverSchedule> findByServiceDateBetweenAndStatusOrderByServiceDateAscStartTimeAsc(
            LocalDate from, LocalDate to, ScheduleStatus status);

    boolean existsByCaregiverIdAndServiceDateAndStartTime(
            Long caregiverId, LocalDate serviceDate, LocalTime startTime);

    /**
     * 原子扣减名额：只有 remaining_quota {@code > 0} 时才会成功，返回 0 表示已被抢光。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CaregiverSchedule s
               set s.remainingQuota = s.remainingQuota - 1
             where s.id = :id
               and s.remainingQuota > 0
            """)
    int tryReserve(@Param("id") Long id);

    /**
     * 释放名额：取消订单或支付超时时调用，不会超过总名额。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CaregiverSchedule s
               set s.remainingQuota = s.remainingQuota + 1
             where s.id = :id
               and s.remainingQuota < s.totalQuota
            """)
    int release(@Param("id") Long id);
}
