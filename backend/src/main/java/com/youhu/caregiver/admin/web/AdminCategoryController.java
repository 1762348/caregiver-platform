package com.youhu.caregiver.admin.web;

import com.youhu.caregiver.admin.dto.AdminCategoryView;
import com.youhu.caregiver.admin.dto.CategoryRequest;
import com.youhu.caregiver.admin.service.AdminCategoryService;
import com.youhu.caregiver.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ApiResponse<List<AdminCategoryView>> list() {
        return ApiResponse.ok(adminCategoryService.list());
    }

    @PostMapping
    public ApiResponse<AdminCategoryView> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(adminCategoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCategoryView> update(@PathVariable Long id,
                                                 @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(adminCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return ApiResponse.ok();
    }
}
