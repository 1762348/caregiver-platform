package com.youhu.caregiver.order.web;

import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.order.domain.ProgressAction;
import com.youhu.caregiver.order.dto.OrderView;
import com.youhu.caregiver.order.service.OrderService;
import com.youhu.caregiver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 护工端：查看自己的订单并更新服务进度 */
@RestController
@RequestMapping("/api/v1/caregiver/orders")
@PreAuthorize("hasRole('CAREGIVER')")
@RequiredArgsConstructor
public class CaregiverOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<PageResult<OrderView>> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.listForCaregiver(user.id(), page, size));
    }

    @PostMapping("/{id}/depart")
    public ApiResponse<OrderView> depart(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.updateProgress(user.id(), id, ProgressAction.DEPART));
    }

    @PostMapping("/{id}/arrive")
    public ApiResponse<OrderView> arrive(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.updateProgress(user.id(), id, ProgressAction.ARRIVE));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<OrderView> start(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.updateProgress(user.id(), id, ProgressAction.START));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderView> complete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.updateProgress(user.id(), id, ProgressAction.COMPLETE));
    }
}
