package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.common.domain.Gender;
import com.youhu.caregiver.common.domain.RecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CaregiverRequest(
        @NotBlank(message = "请输入手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "请输入姓名")
        @Size(max = 50, message = "姓名过长")
        String name,

        @NotNull(message = "请选择性别")
        Gender gender,

        LocalDate birth,

        @NotBlank(message = "请输入工号")
        @Size(max = 30, message = "工号过长")
        String code,

        Long categoryId,

        @Size(max = 1000, message = "资质描述过长")
        String qualification,

        @Size(max = 1000, message = "简介过长")
        String intro,

        @NotNull(message = "请输入服务价格")
        @DecimalMin(value = "0", message = "价格不能为负")
        BigDecimal price,

        RecordStatus status) {
}
