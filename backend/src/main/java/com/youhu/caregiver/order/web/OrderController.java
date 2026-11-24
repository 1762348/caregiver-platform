package com.youhu.caregiver.order.web;

import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.order.domain.OrderStatus;
import com.youhu.caregiver.order.dto.CancelRequest;
import com.youhu.caregiver.order.dto.CancelResult;
import com.youhu.caregiver.order.dto.CreateOrderRequest;
import com.youhu.caregiver.order.dto.OrderView;
import com.youhu.caregiver.order.dto.PayRequest;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 创建订单（锁定名额），订单状态为待支付 */
    @PostMapping
    public ApiResponse<OrderView> create(@AuthenticationPrincipal AuthUser user,
                                         @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(user.id(), request));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderView>> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.listMine(user.id(), status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderView> detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.get(user.id(), id));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderView> pay(@AuthenticationPrincipal AuthUser user,
                                      @PathVariable Long id,
                                      @Valid @RequestBody PayRequest request) {
        return ApiResponse.ok(orderService.pay(user.id(), id, request.channel()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<CancelResult> cancel(@AuthenticationPrincipal AuthUser user,
                                            @PathVariable Long id,
                                            @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.cancel(user.id(), id, reason));
    }
}
