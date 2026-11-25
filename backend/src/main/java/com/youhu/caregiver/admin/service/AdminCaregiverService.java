package com.youhu.caregiver.admin.service;

import com.youhu.caregiver.admin.dto.AdminCaregiverView;
import com.youhu.caregiver.admin.dto.CaregiverRequest;
import com.youhu.caregiver.caregiver.domain.Caregiver;
import com.youhu.caregiver.caregiver.repository.CaregiverRepository;
import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.category.repository.CategoryRepository;
import com.youhu.caregiver.common.api.PageResult;
import com.youhu.caregiver.common.domain.RecordStatus;
import com.youhu.caregiver.common.exception.BusinessException;
import com.youhu.caregiver.user.domain.User;
import com.youhu.caregiver.user.domain.UserRole;
import com.youhu.caregiver.user.repository.UserRepository;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端护工档案维护：创建护工同时开通 CAREGIVER 账号 */
@Service
@RequiredArgsConstructor
public class AdminCaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResult<AdminCaregiverView> list(String keyword, RecordStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Caregiver> caregivers = caregiverRepository.search(
                keyword == null || keyword.isBlank() ? null : keyword.trim(), status, pageable);

        Map<Long, String> categoryNames = categoryRepository.findAllById(
                        caregivers.getContent().stream().map(Caregiver::getCategoryId)
                                .filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(Category::getId, Category::getName));

        return PageResult.of(caregivers.map(caregiver -> toView(caregiver, categoryNames)));
    }

    @Transactional
    public AdminCaregiverView create(CaregiverRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw BusinessException.conflict("该手机号已注册");
        }
        if (caregiverRepository.existsByCode(request.code())) {
            throw BusinessException.conflict("工号已存在");
        }

        User user = new User();
        user.setPhone(request.phone().trim());
        user.setName(request.name().trim());
        user.setRole(UserRole.CAREGIVER);
        User savedUser = userRepository.save(user);

        Caregiver caregiver = new Caregiver();
        caregiver.setUserId(savedUser.getId());
        apply(caregiver, request);
        Caregiver saved = caregiverRepository.save(caregiver);
        return toView(saved, categoryNames(saved.getCategoryId()));
    }

    @Transactional
    public AdminCaregiverView update(Long id, CaregiverRequest request) {
        Caregiver caregiver = caregiverRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("护工不存在"));
        caregiverRepository.findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw BusinessException.conflict("工号已存在");
                });

        User user = userRepository.findById(caregiver.getUserId())
                .orElseThrow(() -> BusinessException.notFound("护工账号不存在"));
        if (!user.getPhone().equals(request.phone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw BusinessException.conflict("该手机号已被占用");
            }
            user.setPhone(request.phone().trim());
        }
        user.setName(request.name().trim());
        userRepository.save(user);

        apply(caregiver, request);
        return toView(caregiverRepository.save(caregiver), categoryNames(caregiver.getCategoryId()));
    }

    @Transactional
    public AdminCaregiverView updateStatus(Long id, RecordStatus status) {
        Caregiver caregiver = caregiverRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("护工不存在"));
        caregiver.setStatus(status);
        return toView(caregiverRepository.save(caregiver), categoryNames(caregiver.getCategoryId()));
    }

    private void apply(Caregiver caregiver, CaregiverRequest request) {
        caregiver.setCode(request.code().trim());
        caregiver.setName(request.name().trim());
        caregiver.setGender(request.gender());
        caregiver.setBirth(request.birth());
        caregiver.setPhone(request.phone().trim());
        caregiver.setCategoryId(request.categoryId());
        caregiver.setQualification(request.qualification());
        caregiver.setIntro(request.intro());
        caregiver.setPrice(request.price());
        caregiver.setStatus(request.status() == null ? RecordStatus.ACTIVE : request.status());
    }

    private Map<Long, String> categoryNames(Long categoryId) {
        if (categoryId == null) {
            return Map.of();
        }
        return categoryRepository.findById(categoryId)
                .map(category -> Map.of(category.getId(), category.getName()))
                .orElseGet(Map::of);
    }

    private AdminCaregiverView toView(Caregiver caregiver, Map<Long, String> categoryNames) {
        return new AdminCaregiverView(
                caregiver.getId(),
                caregiver.getUserId(),
                caregiver.getCode(),
                caregiver.getName(),
                caregiver.getGender(),
                caregiver.getBirth(),
                caregiver.getPhone(),
                caregiver.getCategoryId(),
                caregiver.getCategoryId() == null ? null : categoryNames.get(caregiver.getCategoryId()),
                caregiver.getPrice(),
                caregiver.getStatus(),
                caregiver.getRating(),
                caregiver.getReviews(),
                caregiver.getCreatedAt());
    }
}
