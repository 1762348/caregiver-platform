package com.youhu.caregiver.caregiver.service;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.dto.CaregiverProfileView;
import com.youhu.caregiver.caregiver.dto.CaregiverScheduleView;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 护工端：个人资料与自己的排班 */
@Service
@RequiredArgsConstructor
public class CaregiverPortalService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CaregiverRepository caregiverRepository;
    private final CaregiverScheduleRepository scheduleRepository;
    private final CategoryRepository categoryRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CaregiverProfileView profile(Long userId) {
        Caregiver caregiver = caregiver(userId);
        String categoryName = caregiver.getCategoryId() == null ? null
                : categoryRepository.findById(caregiver.getCategoryId())
                        .map(Category::getName).orElse(null);
        return new CaregiverProfileView(
                caregiver.getId(), caregiver.getCode(), caregiver.getName(), caregiver.getPhone(),
                categoryName, caregiver.getPrice(), caregiver.getRating(), caregiver.getReviews(),
                caregiver.getStatus());
    }

    @Transactional(readOnly = true)
    public List<CaregiverScheduleView> schedules(Long userId, LocalDate from, LocalDate to) {
        Caregiver caregiver = caregiver(userId);
        LocalDate start = from == null ? LocalDate.now(clock) : from;
        LocalDate end = to == null ? start.plusDays(13) : to;
        if (end.isBefore(start)) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        return scheduleRepository
                .findByCaregiverIdAndServiceDateBetweenOrderByServiceDateAscStartTimeAsc(
                        caregiver.getId(), start, end)
                .stream()
                .map(this::toView)
                .toList();
    }

    private Caregiver caregiver(Long userId) {
        return caregiverRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(403, "当前账号未绑定护工档案"));
    }

    private CaregiverScheduleView toView(CaregiverSchedule schedule) {
        return new CaregiverScheduleView(
                schedule.getId(),
                schedule.getServiceDate(),
                schedule.getStartTime().format(TIME_FORMAT) + "-"
                        + schedule.getEndTime().format(TIME_FORMAT),
                schedule.getTotalQuota(),
                schedule.getRemainingQuota(),
                schedule.getPrice(),
                schedule.getStatus());
    }
}
