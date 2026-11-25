package com.youhu.caregiver.admin.dto;

import com.youhu.caregiver.common.domain.RecordStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "请输入类别编码")
        @Size(max = 30, message = "编码过长")
        String code,

        @NotBlank(message = "请输入类别名称")
        @Size(max = 50, message = "名称过长")
        String name,

        @Size(max = 500, message = "描述过长")
        String description,

        @NotNull(message = "请输入基础价格")
        @DecimalMin(value = "0", message = "基础价格不能为负")
        BigDecimal basePrice,

        @NotNull(message = "请选择状态")
        RecordStatus status) {
}
