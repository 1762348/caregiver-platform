package com.youhu.caregiver.receiver.domain;

import com.youhu.caregiver.common.domain.CreatedEntity;
import com.youhu.caregiver.common.domain.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 被照顾人档案（所属用户） */
@Entity
@Table(name = "receivers")
@Getter
@Setter
@NoArgsConstructor
public class Receiver extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "id_card", nullable = false, length = 18)
    private String idCard;

    @Column(length = 11)
    private String phone;

    @Column(length = 20)
    private String relation;

    @Column(length = 1000)
    private String health;

    @Column(length = 1000)
    private String special;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    private LocalDate birth;
}
