package com.youhu.caregiver.admin.web;

import com.youhu.caregiver.admin.dto.AdminCaregiverView;
import com.youhu.caregiver.admin.dto.CaregiverRequest;
import com.youhu.caregiver.admin.service.AdminCaregiverService;
import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.common.domain.RecordStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/caregivers")
@RequiredArgsConstructor
public class AdminCaregiverController {

    private final AdminCaregiverService adminCaregiverService;

    @GetMapping
    public ApiResponse<PageResult<AdminCaregiverView>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecordStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminCaregiverService.list(keyword, status, page, size));
    }

    @PostMapping
    public ApiResponse<AdminCaregiverView> create(@Valid @RequestBody CaregiverRequest request) {
        return ApiResponse.ok(adminCaregiverService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCaregiverView> update(@PathVariable Long id,
                                                  @Valid @RequestBody CaregiverRequest request) {
        return ApiResponse.ok(adminCaregiverService.update(id, request));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<AdminCaregiverView> updateStatus(@PathVariable Long id,
                                                        @RequestParam RecordStatus status) {
        return ApiResponse.ok(adminCaregiverService.updateStatus(id, status));
    }
}
