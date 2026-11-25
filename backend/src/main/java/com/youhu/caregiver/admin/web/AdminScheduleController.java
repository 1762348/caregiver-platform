package com.youhu.caregiver.admin.web;

import com.youhu.caregiver.admin.dto.AdminScheduleView;
import com.youhu.caregiver.admin.dto.GenerateScheduleRequest;
import com.youhu.caregiver.admin.dto.QuotaRequest;
import com.youhu.caregiver.admin.dto.ScheduleRequest;
import com.youhu.caregiver.admin.dto.StopScheduleRequest;
import com.youhu.caregiver.admin.dto.StopScheduleResult;
import com.youhu.caregiver.admin.service.AdminScheduleService;
import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.security.AuthUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    @GetMapping
    public ApiResponse<List<AdminScheduleView>> list(
            @RequestParam Long caregiverId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminScheduleService.list(caregiverId, from, to));
    }

    @PostMapping
    public ApiResponse<AdminScheduleView> create(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(adminScheduleService.create(request));
    }

    @PostMapping("/generate")
    public ApiResponse<Integer> generate(@Valid @RequestBody GenerateScheduleRequest request) {
        return ApiResponse.ok(adminScheduleService.generate(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminScheduleView> update(@PathVariable Long id,
                                                 @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(adminScheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminScheduleService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/quota")
    public ApiResponse<AdminScheduleView> quota(@PathVariable Long id,
                                                @Valid @RequestBody QuotaRequest request) {
        return ApiResponse.ok(adminScheduleService.adjustQuota(id, request.remainingQuota()));
    }

    @PostMapping("/{id}/stop")
    public ApiResponse<StopScheduleResult> stop(
            @PathVariable Long id,
            @RequestBody(required = false) StopScheduleRequest request,
            @AuthenticationPrincipal AuthUser user) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(adminScheduleService.stop(id, reason, user.id()));
    }
}
