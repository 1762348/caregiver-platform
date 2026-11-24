package com.youhu.caregiver.catalog.web;

import com.youhu.caregiver.catalog.dto.CaregiverView;
import com.youhu.caregiver.catalog.dto.CategoryView;
import com.youhu.caregiver.catalog.dto.ScheduleView;
import com.youhu.caregiver.catalog.service.CatalogService;
import com.youhu.caregiver.common.api.ApiResponse;
import com.youhu.caregiver.common.api.PageResult;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 公开的目录查询：服务类别、护工、可预约排班 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.ok(catalogService.listCategories());
    }

    @GetMapping("/caregivers")
    public ApiResponse<PageResult<CaregiverView>> caregivers(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(catalogService.listCaregivers(categoryId, page, size));
    }

    @GetMapping("/caregivers/{id}")
    public ApiResponse<CaregiverView> caregiver(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.getCaregiver(id));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleView>> schedules(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(catalogService.listSchedules(categoryId, date));
    }
}
