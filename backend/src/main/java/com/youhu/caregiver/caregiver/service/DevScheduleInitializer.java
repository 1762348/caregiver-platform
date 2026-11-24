package com.youhu.caregiver.caregiver.service;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.domain.CaregiverBaseShift;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverBaseShiftRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开发环境排班生成器：首次启动且排班为空时，按基础时段生成未来 14 天排班，方便本地演示。
 * 生产环境不生效，正式排班由管理端维护（P5）。
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevScheduleInitializer implements ApplicationRunner {

    private static final int DAYS = 14;
    private static final int DEFAULT_QUOTA = 3;

    private final CaregiverRepository caregiverRepository;
    private final CaregiverBaseShiftRepository baseShiftRepository;
    private final CaregiverScheduleRepository scheduleRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (scheduleRepository.count() > 0) {
            return;
        }
        int created = 0;
        LocalDate today = LocalDate.now(clock);
        for (Caregiver caregiver : caregiverRepository.findByStatus(RecordStatus.ACTIVE)) {
            for (CaregiverBaseShift shift : baseShiftRepository
                    .findByCaregiverIdAndStatusOrderByStartTimeAsc(caregiver.getId(), RecordStatus.ACTIVE)) {
                for (int day = 0; day < DAYS; day++) {
                    LocalDate date = today.plusDays(day);
                    if (!LocalDateTime.of(date, shift.getStartTime()).isAfter(LocalDateTime.now(clock))) {
                        continue;
                    }
                    if (scheduleRepository.existsByCaregiverIdAndServiceDateAndStartTime(
                            caregiver.getId(), date, shift.getStartTime())) {
                        continue;
                    }
                    CaregiverSchedule schedule = new CaregiverSchedule();
                    schedule.setCaregiverId(caregiver.getId());
                    schedule.setShiftId(shift.getId());
                    schedule.setServiceDate(date);
                    schedule.setStartTime(shift.getStartTime());
                    schedule.setEndTime(shift.getEndTime());
                    schedule.setTotalQuota(DEFAULT_QUOTA);
                    schedule.setRemainingQuota(DEFAULT_QUOTA);
                    schedule.setPrice(shift.getPrice());
                    schedule.setStatus(ScheduleStatus.OPEN);
                    scheduleRepository.save(schedule);
                    created++;
                }
            }
        }
        log.info("开发环境已生成 {} 条排班（未来 {} 天）", created, DAYS);
    }
}
