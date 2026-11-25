package com.youhu.caregiver.caregiver.web;

import com.youhu.caregiver.caregiver.dto.CaregiverProfileView;
import com.youhu.caregiver.caregiver.dto.CaregiverScheduleView;
import com.youhu.caregiver.caregiver.service.CaregiverPortalService;
import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.security.AuthUser;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 护工端：个人资料与排班 */
@RestController
@RequestMapping("/api/v1/caregiver")
@PreAuthorize("hasRole('CAREGIVER')")
@RequiredArgsConstructor
public class CaregiverPortalController {

    private final CaregiverPortalService caregiverPortalService;

    @GetMapping("/me")
    public ApiResponse<CaregiverProfileView> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(caregiverPortalService.profile(user.id()));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<CaregiverScheduleView>> schedules(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(caregiverPortalService.schedules(user.id(), from, to));
    }
}
