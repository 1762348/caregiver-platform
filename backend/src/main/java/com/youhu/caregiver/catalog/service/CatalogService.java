package com.youhu.caregiver.catalog.service;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.catalog.dto.CaregiverView;
import com.youhu.caregiver.catalog.dto.CategoryView;
import com.youhu.caregiver.catalog.dto.ScheduleView;
import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.common.domain.RecordStatus;
import com.youhu.caregiver.common.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 服务类别、护工与排班查询 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CategoryRepository categoryRepository;
    private final CaregiverRepository caregiverRepository;
    private final CaregiverScheduleRepository scheduleRepository;
    private final Clock clock;

    public List<CategoryView> listCategories() {
        return categoryRepository.findByStatusOrderByBasePriceAsc(RecordStatus.ACTIVE).stream()
                .map(category -> new CategoryView(category.getId(), category.getCode(),
                        category.getName(), category.getDescription(), category.getBasePrice()))
                .toList();
    }

    public PageResult<CaregiverView> listCaregivers(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Caregiver> caregivers = categoryId == null
                ? caregiverRepository.findByStatus(RecordStatus.ACTIVE, pageable)
                : caregiverRepository.findByCategoryIdAndStatus(categoryId, RecordStatus.ACTIVE, pageable);

        Map<Long, String> categoryNames = categoryRepository.findAllById(
                        caregivers.getContent().stream()
                                .map(Caregiver::getCategoryId)
                                .filter(java.util.Objects::nonNull)
                                .toList())
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return PageResult.of(caregivers.map(caregiver -> toView(caregiver, categoryNames)));
    }

    public CaregiverView getCaregiver(Long id) {
        Caregiver caregiver = caregiverRepository.findById(id)
                .filter(item -> item.getStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("护工不存在或已停用"));
        Map<Long, String> categoryNames = new java.util.HashMap<>();
        if (caregiver.getCategoryId() != null) {
            categoryRepository.findById(caregiver.getCategoryId())
                    .ifPresent(category -> categoryNames.put(category.getId(), category.getName()));
        }
        return toView(caregiver, categoryNames);
    }

    public List<ScheduleView> listSchedules(Long categoryId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now(clock) : date;
        if (targetDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(400, "不能查询过去的排班");
        }

        List<CaregiverSchedule> schedules;
        if (categoryId == null) {
            schedules = scheduleRepository
                    .findByServiceDateAndStatusAndRemainingQuotaGreaterThanOrderByStartTimeAsc(
                            targetDate, ScheduleStatus.OPEN, 0);
        } else {
            List<Long> caregiverIds = caregiverRepository
                    .findByCategoryIdAndStatus(categoryId, RecordStatus.ACTIVE).stream()
                    .map(Caregiver::getId)
                    .toList();
            if (caregiverIds.isEmpty()) {
                return List.of();
            }
            schedules = scheduleRepository
                    .findByCaregiverIdInAndServiceDateAndStatusAndRemainingQuotaGreaterThanOrderByStartTimeAsc(
                            caregiverIds, targetDate, ScheduleStatus.OPEN, 0);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, Caregiver> caregivers = caregiverRepository.findAllById(
                        schedules.stream().map(CaregiverSchedule::getCaregiverId).distinct().toList())
                .stream().collect(Collectors.toMap(Caregiver::getId, Function.identity()));
        Map<Long, String> categoryNames = categoryRepository.findAllById(
                        caregivers.values().stream().map(Caregiver::getCategoryId)
                                .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));

        return schedules.stream()
                .filter(schedule -> LocalDateTime.of(schedule.getServiceDate(), schedule.getStartTime())
                        .isAfter(now))
                .map(schedule -> {
                    Caregiver caregiver = caregivers.get(schedule.getCaregiverId());
                    String categoryName = caregiver == null || caregiver.getCategoryId() == null
                            ? null : categoryNames.get(caregiver.getCategoryId());
                    return new ScheduleView(
                            schedule.getId(),
                            schedule.getCaregiverId(),
                            caregiver == null ? null : caregiver.getName(),
                            caregiver == null ? null : caregiver.getCategoryId(),
                            categoryName,
                            schedule.getServiceDate(),
                            format(schedule.getStartTime()),
                            format(schedule.getEndTime()),
                            timeRange(schedule.getStartTime(), schedule.getEndTime()),
                            schedule.getRemainingQuota(),
                            schedule.getTotalQuota(),
                            schedule.getPrice());
                })
                .toList();
    }

    private CaregiverView toView(Caregiver caregiver, Map<Long, String> categoryNames) {
        Integer age = caregiver.getBirth() == null ? null
                : Period.between(caregiver.getBirth(), LocalDate.now(clock)).getYears();
        return new CaregiverView(
                caregiver.getId(),
                caregiver.getCode(),
                caregiver.getName(),
                caregiver.getGender() == null ? null : caregiver.getGender().name(),
                age,
                caregiver.getCategoryId(),
                caregiver.getCategoryId() == null ? null : categoryNames.get(caregiver.getCategoryId()),
                caregiver.getPrice(),
                caregiver.getRating(),
                caregiver.getReviews(),
                caregiver.getQualification(),
                caregiver.getIntro());
    }

    private String format(LocalTime time) {
        return time.format(TIME_FORMAT);
    }

    private String timeRange(LocalTime start, LocalTime end) {
        return format(start) + "-" + format(end);
    }
}
