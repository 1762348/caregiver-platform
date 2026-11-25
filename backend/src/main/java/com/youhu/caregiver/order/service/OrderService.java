package com.youhu.caregiver.order.service;

import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.domain.CaregiverSchedule;
import com.youhu.caregiver.caregiver.domain.ScheduleStatus;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.caregiver.repository.CaregiverScheduleRepository;
import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.message.domain.MessageType;
import com.youhu.caregiver.message.service.MessageService;
import com.youhu.caregiver.order.domain.Order;
import com.youhu.caregiver.order.domain.OrderStage;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.domain.OrderStatusLog;
import com.youhu.caregiver.order.domain.ProgressAction;
import com.youhu.caregiver.order.dto.CancelResult;
import com.youhu.caregiver.order.dto.CreateOrderRequest;
import com.youhu.caregiver.order.dto.OrderView;
import com.youhu.caregiver.order.repository.OrderRepository;
import com.youhu.caregiver.order.repository.OrderStatusLogRepository;
import com.youhu.caregiver.payment.domain.PaymentChannel;
import com.youhu.caregiver.payment.service.PaymentResult;
import com.youhu.caregiver.payment.service.PaymentService;
import com.youhu.caregiver.receiver.domain.Receiver;
import com.youhu.caregiver.receiver.repository.ReceiverRepository;
import com.youhu.caregiver.user.domain.UserRole;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 预约、支付、取消、进度更新的核心业务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final int PAYMENT_TIMEOUT_MINUTES = 30;

    private static final Set<OrderStatus> ACTIVE_STATUSES =
            Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.IN_SERVICE);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderRepository orderRepository;
    private final OrderStatusLogRepository orderStatusLogRepository;
    private final ReceiverRepository receiverRepository;
    private final CaregiverScheduleRepository scheduleRepository;
    private final CaregiverRepository caregiverRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentService paymentService;
    private final MessageService messageService;
    private final RefundCalculator refundCalculator;
    private final Clock clock;

    // ---------------------------------------------------------------- 预约

    @Transactional
    public OrderView create(Long userId, CreateOrderRequest request) {
        Receiver receiver = receiverRepository.findByIdAndUserId(request.receiverId(), userId)
                .orElseThrow(() -> BusinessException.notFound("被照顾人不存在"));
        CaregiverSchedule schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> BusinessException.notFound("排班不存在"));

        validateBookable(schedule);
        String serviceTime = schedule.getStartTime().format(TIME_FORMAT) + "-"
                + schedule.getEndTime().format(TIME_FORMAT);

        if (orderRepository.existsByReceiverIdAndServiceDateAndServiceTimeAndStatusIn(
                receiver.getId(), schedule.getServiceDate(), serviceTime, ACTIVE_STATUSES)) {
            throw BusinessException.conflict("该被照顾人在此时段已有预约");
        }
        if (scheduleRepository.tryReserve(schedule.getId()) == 0) {
            throw BusinessException.conflict("该时段已被约满，请选择其他时段");
        }

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setReceiverId(receiver.getId());
        order.setCaregiverId(schedule.getCaregiverId());
        order.setScheduleId(schedule.getId());
        order.setServiceDate(schedule.getServiceDate());
        order.setServiceTime(serviceTime);
        order.setAmount(schedule.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        Order saved = orderRepository.save(order);
        logStatus(saved, null, OrderStatus.PENDING_PAYMENT, userId, UserRole.USER, "创建订单");
        messageService.notifyUser(userId, MessageType.ORDER, "预约成功",
                "订单 " + saved.getOrderNo() + " 已创建，请在 30 分钟内完成支付",
                "/orders/" + saved.getId());
        return toView(saved);
    }

    private void validateBookable(CaregiverSchedule schedule) {
        if (schedule.getStatus() != ScheduleStatus.OPEN) {
            throw BusinessException.conflict("该时段已停止预约");
        }
        if (schedule.getRemainingQuota() == null || schedule.getRemainingQuota() <= 0) {
            throw BusinessException.conflict("该时段已约满，请选择其他时段");
        }
        LocalDateTime start = LocalDateTime.of(schedule.getServiceDate(), schedule.getStartTime());
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(400, "该时段已开始，无法预约");
        }
    }

    // ---------------------------------------------------------------- 支付

    @Transactional
    public OrderView pay(Long userId, Long orderId, PaymentChannel channel) {
        Order order = ownedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw BusinessException.conflict("当前订单状态不可支付");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (order.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES).isBefore(now)) {
            throw new BusinessException(400, "订单已超过支付时限，请重新预约");
        }

        PaymentResult result = paymentService.pay(order, channel);
        if (!result.success()) {
            throw new BusinessException(400, "支付失败：" + result.message());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(now);
        orderRepository.save(order);
        logStatus(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, userId, UserRole.USER, "支付成功");
        messageService.notifyUser(userId, MessageType.PAYMENT, "支付成功",
                "订单 " + order.getOrderNo() + " 已支付 " + order.getAmount() + " 元",
                "/orders/" + order.getId());
        messageService.notifyCaregiver(order.getCaregiverId(), MessageType.ORDER, "新订单待服务",
                "您有一个新订单：" + order.getServiceDate() + " " + order.getServiceTime(),
                "/caregiver/orders");
        return toView(order);
    }

    // ---------------------------------------------------------------- 取消

    @Transactional
    public CancelResult cancel(Long userId, Long orderId, String reason) {
        Order order = ownedOrder(userId, orderId);
        String cancelReason = reason == null || reason.isBlank() ? "用户主动取消" : reason;
        return doCancel(order, userId, UserRole.USER, cancelReason);
    }

    @Transactional
    public int expireOverdue() {
        LocalDateTime deadline = LocalDateTime.now(clock).minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> overdue = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT, deadline);
        int cancelled = 0;
        for (Order order : overdue) {
            try {
                doCancel(order, null, null, "超时未支付，系统自动取消");
                cancelled++;
            } catch (BusinessException ex) {
                log.warn("订单 {} 超时取消失败：{}", order.getOrderNo(), ex.getMessage());
            }
        }
        return cancelled;
    }

    private CancelResult doCancel(Order order, Long operatorId, UserRole role, String reason) {
        if (!order.getStatus().isCancellable()) {
            throw BusinessException.conflict("当前订单状态不可取消");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isBefore(refundCalculator.serviceStart(order))) {
            throw new BusinessException(400, "服务已开始，无法取消");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        boolean feeApplied = false;
        OrderStatus previous = order.getStatus();
        if (previous == OrderStatus.PAID) {
            RefundResult refund = refundCalculator.calculate(order, now);
            refundAmount = refund.refundAmount();
            feeApplied = refund.feeApplied();
            refundAmount = paymentService.refund(order, refundAmount);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(now);
        order.setCancelReason(reason);
        orderRepository.save(order);
        scheduleRepository.release(order.getScheduleId());
        logStatus(order, previous, OrderStatus.CANCELLED, operatorId, role, reason);
        messageService.notifyUser(order.getUserId(), MessageType.ORDER, "订单已取消",
                "订单 " + order.getOrderNo() + " 已取消"
                        + (refundAmount.signum() > 0 ? "，退款 " + refundAmount + " 元" : ""),
                "/orders/" + order.getId());
        return new CancelResult(toView(order), refundAmount, feeApplied);
    }

    /**
     * 管理端强制取消（停班等场景）：不受 2 小时规则限制，已支付订单全额退款。
     */
    @Transactional
    public CancelResult forceCancel(Long orderId, Long operatorId, UserRole role, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!order.getStatus().isCancellable()) {
            throw BusinessException.conflict("当前订单状态不可取消");
        }
        OrderStatus previous = order.getStatus();
        BigDecimal refundAmount = BigDecimal.ZERO;
        if (previous == OrderStatus.PAID) {
            refundAmount = paymentService.refund(order, order.getAmount());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(now);
        order.setCancelReason(reason);
        orderRepository.save(order);
        scheduleRepository.release(order.getScheduleId());
        logStatus(order, previous, OrderStatus.CANCELLED, operatorId, role, reason);
        messageService.notifyUser(order.getUserId(), MessageType.ORDER, "订单已取消",
                "订单 " + order.getOrderNo() + " 因" + reason + "已取消"
                        + (refundAmount.signum() > 0 ? "，退款 " + refundAmount + " 元" : ""),
                "/orders/" + order.getId());
        return new CancelResult(toView(order), refundAmount, false);
    }

    // ---------------------------------------------------------------- 查询
    @Transactional(readOnly = true)
    public OrderView get(Long userId, Long orderId) {
        return toView(ownedOrder(userId, orderId));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderView> listMine(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Order> orders = status == null
                ? orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return PageResult.of(orders.map(this::toView));
    }

    @Transactional(readOnly = true)
    public PageResult<OrderView> listForCaregiver(Long userId, int page, int size) {
        Caregiver caregiver = currentCaregiver(userId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return PageResult.of(orderRepository
                .findByCaregiverIdOrderByCreatedAtDesc(caregiver.getId(), pageable)
                .map(this::toView));
    }

    // ---------------------------------------------------------------- 服务进度

    @Transactional
    public OrderView updateProgress(Long userId, Long orderId, ProgressAction action) {
        Caregiver caregiver = currentCaregiver(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        if (!caregiver.getId().equals(order.getCaregiverId())) {
            throw new BusinessException(403, "无权操作该订单");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        switch (action) {
            case DEPART -> {
                requireStatus(order, OrderStatus.PAID, "确认出发");
                if (order.getDepartedAt() != null) {
                    throw BusinessException.conflict("已确认出发，请勿重复操作");
                }
                order.setDepartedAt(now);
                notifyProgress(order, "护工已出发", "护工正在前往服务地点");
            }
            case ARRIVE -> {
                requireStatus(order, OrderStatus.PAID, "确认到达");
                if (order.getDepartedAt() == null) {
                    throw BusinessException.conflict("请先确认出发");
                }
                if (order.getArrivedAt() != null) {
                    throw BusinessException.conflict("已确认到达，请勿重复操作");
                }
                order.setArrivedAt(now);
                notifyProgress(order, "护工已到达", "护工已到达服务地点，即将开始服务");
            }
            case START -> {
                requireStatus(order, OrderStatus.PAID, "开始服务");
                if (order.getArrivedAt() == null) {
                    throw BusinessException.conflict("请先确认到达");
                }
                order.setStartedAt(now);
                transition(order, OrderStatus.IN_SERVICE, userId, UserRole.CAREGIVER, "开始服务");
                notifyProgress(order, "服务已开始", "护工已开始本次服务");
            }
            case COMPLETE -> {
                requireStatus(order, OrderStatus.IN_SERVICE, "完成服务");
                order.setCompletedAt(now);
                transition(order, OrderStatus.COMPLETED, userId, UserRole.CAREGIVER, "服务完成");
                notifyProgress(order, "服务已完成", "本次服务已完成，感谢您的信任");
            }
        }

        Order saved = orderRepository.save(order);
        return toView(saved);
    }

    private void requireStatus(Order order, OrderStatus expected, String action) {
        if (order.getStatus() != expected) {
            throw BusinessException.conflict("当前订单状态不支持「" + action + "」");
        }
    }

    private void transition(Order order, OrderStatus target, Long operatorId, UserRole role, String remark) {
        OrderStatus previous = order.getStatus();
        if (!previous.canTransitTo(target)) {
            throw BusinessException.conflict("订单状态不允许从 " + previous + " 流转到 " + target);
        }
        order.setStatus(target);
        logStatus(order, previous, target, operatorId, role, remark);
    }

    private void notifyProgress(Order order, String title, String body) {
        messageService.notifyUser(order.getUserId(), MessageType.SERVICE, title,
                "订单 " + order.getOrderNo() + "：" + body, "/orders/" + order.getId());
    }

    // ---------------------------------------------------------------- 内部方法

    private Caregiver currentCaregiver(Long userId) {
        return caregiverRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(403, "当前账号未绑定护工档案"));
    }

    private Order ownedOrder(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
    }

    private void logStatus(Order order, OrderStatus from, OrderStatus to, Long operatorId,
                           UserRole role, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorRole(role);
        statusLog.setRemark(remark);
        orderStatusLogRepository.save(statusLog);
    }

    private String nextOrderNo() {
        return "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private OrderView toView(Order order) {
        Caregiver caregiver = caregiverRepository.findById(order.getCaregiverId()).orElse(null);
        Receiver receiver = receiverRepository.findById(order.getReceiverId()).orElse(null);
        String categoryName = null;
        if (caregiver != null && caregiver.getCategoryId() != null) {
            categoryName = categoryRepository.findById(caregiver.getCategoryId())
                    .map(Category::getName).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean canPay = order.getStatus() == OrderStatus.PENDING_PAYMENT
                && order.getCreatedAt() != null
                && order.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES).isAfter(now);
        boolean canCancel = order.getStatus().isCancellable()
                && now.isBefore(refundCalculator.serviceStart(order));

        return new OrderView(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                OrderStage.from(order),
                order.getAmount(),
                order.getServiceDate(),
                order.getServiceTime(),
                order.getCaregiverId(),
                caregiver == null ? null : caregiver.getName(),
                caregiver == null ? null : caregiver.getPhone(),
                categoryName,
                order.getReceiverId(),
                receiver == null ? null : receiver.getName(),
                order.getCreatedAt(),
                order.getPayTime(),
                order.getCancelTime(),
                order.getDepartedAt(),
                order.getArrivedAt(),
                order.getStartedAt(),
                order.getCompletedAt(),
                order.getCancelReason(),
                canPay,
                canCancel);
    }
}
