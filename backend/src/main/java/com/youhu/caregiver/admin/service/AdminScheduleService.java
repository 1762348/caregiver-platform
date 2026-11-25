package com.youhu.caregiver.admin.service;

import com.youhu.caregiver.admin.dto.AdminScheduleView;
import com.youhu.caregiver.admin.dto.GenerateScheduleRequest;
import com.youhu.caregiver.admin.dto.ScheduleRequest;
import com.youhu.caregiver.admin.dto.StopScheduleResult;
import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.domain.CaregiverBaseShift;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverBaseShiftRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.common.domain.RecordStatus;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.order.domain.Order;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.user.domain.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端排班与名额维护、停班处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminScheduleService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES =
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.IN_SERVICE);

    private final CaregiverScheduleRepository scheduleRepository;
    private final CaregiverRepository caregiverRepository;
    private final CaregiverBaseShiftRepository baseShiftRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminScheduleView> list(Long caregiverId, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? LocalDate.now(clock) : from;
        LocalDate end = to == null ? start.plusDays(13) : to;
        if (end.isBefore(start)) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        return scheduleRepository
                .findByCaregiverIdAndServiceDateBetweenOrderByServiceDateAscStartTimeAsc(
                        caregiverId, start, end)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public AdminScheduleView create(ScheduleRequest request) {
        Caregiver caregiver = activeCaregiver(request.caregiverId());
        validateTimes(request.startTime(), request.endTime());
        validateFuture(request.serviceDate(), request.startTime());
        if (scheduleRepository.existsByCaregiverIdAndServiceDateAndStartTime(
                caregiver.getId(), request.serviceDate(), request.startTime())) {
            throw BusinessException.conflict("该护工当天此时段已有排班");
        }
        CaregiverSchedule schedule = new CaregiverSchedule();
        schedule.setCaregiverId(caregiver.getId());
        schedule.setServiceDate(request.serviceDate());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setTotalQuota(request.totalQuota());
        schedule.setRemainingQuota(request.totalQuota());
        schedule.setPrice(request.price());
        schedule.setStatus(request.status() == null ? ScheduleStatus.OPEN : request.status());
        return toView(scheduleRepository.save(schedule));
    }

    /** 按基础时段批量生成排班，已存在的时段自动跳过 */
    @Transactional
    public int generate(GenerateScheduleRequest request) {
        if (request.to().isBefore(request.from())) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        if (request.from().plusDays(31).isBefore(request.to())) {
            throw new BusinessException(400, "单次最多生成 31 天排班");
        }
        Caregiver caregiver = activeCaregiver(request.caregiverId());
        List<CaregiverBaseShift> shifts = baseShiftRepository
                .findByCaregiverIdAndStatusOrderByStartTimeAsc(caregiver.getId(), RecordStatus.ACTIVE);
        if (shifts.isEmpty()) {
            throw BusinessException.conflict("该护工还没有基础时段，请先维护基础时段");
        }

        int created = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (LocalDate date = request.from(); !date.isAfter(request.to()); date = date.plusDays(1)) {
            for (CaregiverBaseShift shift : shifts) {
                if (!LocalDateTime.of(date, shift.getStartTime()).isAfter(now)) {
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
                schedule.setTotalQuota(request.totalQuota());
                schedule.setRemainingQuota(request.totalQuota());
                schedule.setPrice(shift.getPrice());
                schedule.setStatus(ScheduleStatus.OPEN);
                scheduleRepository.save(schedule);
                created++;
            }
        }
        return created;
    }

    @Transactional
    public AdminScheduleView update(Long id, ScheduleRequest request) {
        CaregiverSchedule schedule = schedule(id);
        validateTimes(request.startTime(), request.endTime());
        int booked = schedule.getTotalQuota() - schedule.getRemainingQuota();
        if (request.totalQuota() < booked) {
            throw new BusinessException(400, "名额不能小于已预约数量 " + booked);
        }
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setTotalQuota(request.totalQuota());
        schedule.setRemainingQuota(request.totalQuota() - booked);
        schedule.setPrice(request.price());
        if (request.status() != null) {
            schedule.setStatus(request.status());
        }
        return toView(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        CaregiverSchedule schedule = schedule(id);
        if (orderRepository.countByScheduleId(id) > 0) {
            throw BusinessException.conflict("该排班已有订单，不能删除，可改为停班");
        }
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public AdminScheduleView adjustQuota(Long id, int remainingQuota) {
        CaregiverSchedule schedule = schedule(id);
        if (remainingQuota < 0 || remainingQuota > schedule.getTotalQuota()) {
            throw new BusinessException(400, "剩余名额必须在 0 到 " + schedule.getTotalQuota() + " 之间");
        }
        schedule.setRemainingQuota(remainingQuota);
        return toView(scheduleRepository.save(schedule));
    }

    /** 停班：关闭排班，并全额退款取消该时段全部有效订单 */
    @Transactional
    public StopScheduleResult stop(Long id, String reason, Long operatorId) {
        CaregiverSchedule schedule = schedule(id);
        schedule.setStatus(ScheduleStatus.CLOSED);
        scheduleRepository.save(schedule);

        List<Order> activeOrders = orderRepository
                .findByScheduleIdAndStatusIn(id, ACTIVE_ORDER_STATUSES);
        BigDecimal refundTotal = BigDecimal.ZERO;
        int cancelled = 0;
        String stopReason = "护工停班" + (reason == null || reason.isBlank() ? "" : "：" + reason);
        for (Order order : activeOrders) {
            try {
                refundTotal = refundTotal.add(orderService
                        .forceCancel(order.getId(), operatorId, UserRole.ADMIN, stopReason)
                        .refundAmount());
                cancelled++;
            } catch (BusinessException ex) {
                log.warn("停班取消失败，订单 {}：{}", order.getOrderNo(), ex.getMessage());
            }
        }
        return new StopScheduleResult(id, cancelled, refundTotal);
    }

    private CaregiverSchedule schedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("排班不存在"));
    }

    private Caregiver activeCaregiver(Long caregiverId) {
        return caregiverRepository.findById(caregiverId)
                .filter(caregiver -> caregiver.getStatus() == RecordStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("护工不存在或已停用"));
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
    }

    private void validateFuture(LocalDate date, LocalTime start) {
        if (!LocalDateTime.of(date, start).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(400, "排班时间必须晚于当前时间");
        }
    }

    private AdminScheduleView toView(CaregiverSchedule schedule) {
        String caregiverName = caregiverRepository.findById(schedule.getCaregiverId())
                .map(Caregiver::getName).orElse(null);
        return new AdminScheduleView(
                schedule.getId(),
                schedule.getCaregiverId(),
                caregiverName,
                schedule.getServiceDate(),
                schedule.getStartTime().format(TIME_FORMAT) + "-"
                        + schedule.getEndTime().format(TIME_FORMAT),
                schedule.getTotalQuota(),
                schedule.getRemainingQuota(),
                schedule.getPrice(),
                schedule.getStatus());
    }
}
