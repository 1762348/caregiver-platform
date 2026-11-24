package com.youhu.caregiver.caregiver.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 名额扣减/释放是防超卖的核心，必须由数据库原子操作保证。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleQuotaTest {

    @Autowired
    private CaregiverScheduleRepository scheduleRepository;

    private CaregiverSchedule newSchedule(int totalQuota) {
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(1L);
        schedule.setServiceDate(LocalDate.of(2030, 1, 1));
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setTotalQuota(totalQuota);
        schedule.setRemainingQuota(totalQuota);
        schedule.setPrice(new BigDecimal("200.00"));
        schedule.setStatus(ScheduleStatus.OPEN);
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Test
    void reserveDecreasesQuotaAndStopsAtZero() {
        CaregiverSchedule schedule = newSchedule(1);

        assertThat(scheduleRepository.tryReserve(schedule.getId())).isEqualTo(1);
        assertThat(scheduleRepository.tryReserve(schedule.getId())).isZero();

        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isZero();
    }

    @Test
    void releaseNeverExceedsTotalQuota() {
        CaregiverSchedule schedule = newSchedule(1);
        scheduleRepository.tryReserve(schedule.getId());

        assertThat(scheduleRepository.release(schedule.getId())).isEqualTo(1);
        assertThat(scheduleRepository.release(schedule.getId())).isZero();

        assertThat(scheduleRepository.findById(schedule.getId()).orElseThrow().getRemainingQuota())
                .isEqualTo(1);
    }

    @Test
    void totalQuotaCanBeReservedFully() {
        CaregiverSchedule schedule = newSchedule(2);

        assertThat(scheduleRepository.tryReserve(schedule.getId())).isEqualTo(1);
        assertThat(scheduleRepository.tryReserve(schedule.getId())).isEqualTo(1);
        assertThat(scheduleRepository.tryReserve(schedule.getId())).isZero();
    }
}
