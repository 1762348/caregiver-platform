package com.youhu.caregiver.caregiver.domain;

import com.youhu.caregiver.common.domain.AuditableEntity;
import com.youhu.caregiver.common.domain.Gender;
import com.youhu.caregiver.common.domain.RecordStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 护工档案，与 users 一对一（user_id 唯一） */
@Entity
@Table(name = "caregivers")
@Getter
@Setter
@NoArgsConstructor
public class Caregiver extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    private LocalDate birth;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(length = 1000)
    private String qualification;

    @Column(length = 1000)
    private String intro;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    private Integer reviews;
}
