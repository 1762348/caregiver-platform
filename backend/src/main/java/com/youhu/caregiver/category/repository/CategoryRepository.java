package com.youhu.caregiver.category.repository;

import com.youhu.caregiver.category.domain.Category;
import com.youhu.caregiver.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStatusOrderByBasePriceAsc(RecordStatus status);

    Optional<Category> findByCode(String code);

    boolean existsByName(String name);

    java.util.List<Category> findAllByOrderByBasePriceAsc();
}
