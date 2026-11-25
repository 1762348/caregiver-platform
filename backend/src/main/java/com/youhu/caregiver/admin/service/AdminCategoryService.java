package com.youhu.caregiver.admin.service;

import com.youhu.caregiver.admin.dto.AdminCategoryView;
import com.youhu.caregiver.admin.dto.CategoryRequest;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端服务类别维护 */
@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final CaregiverRepository caregiverRepository;

    @Transactional(readOnly = true)
    public List<AdminCategoryView> list() {
        return categoryRepository.findAllByOrderByBasePriceAsc().stream()
                .map(category -> new AdminCategoryView(
                        category.getId(), category.getCode(), category.getName(),
                        category.getDescription(), category.getBasePrice(), category.getStatus(),
                        caregiverRepository.countByCategoryId(category.getId())))
                .toList();
    }

    @Transactional
    public AdminCategoryView create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw BusinessException.conflict("类别名称已存在");
        }
        if (categoryRepository.findByCode(request.code()).isPresent()) {
            throw BusinessException.conflict("类别编码已存在");
        }
        Category category = new Category();
        apply(category, request);
        Category saved = categoryRepository.save(category);
        return toView(saved);
    }

    @Transactional
    public AdminCategoryView update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("类别不存在"));
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw BusinessException.conflict("类别名称已存在");
        }
        categoryRepository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw BusinessException.conflict("类别编码已存在");
                });
        apply(category, request);
        return toView(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("类别不存在"));
        if (caregiverRepository.existsByCategoryId(id)) {
            throw BusinessException.conflict("该类别下还有护工，不能删除");
        }
        categoryRepository.delete(category);
    }

    private void apply(Category category, CategoryRequest request) {
        category.setCode(request.code().trim());
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setBasePrice(request.basePrice());
        category.setStatus(request.status());
    }

    private AdminCategoryView toView(Category category) {
        return new AdminCategoryView(category.getId(), category.getCode(), category.getName(),
                category.getDescription(), category.getBasePrice(), category.getStatus(),
                caregiverRepository.countByCategoryId(category.getId()));
    }
}
