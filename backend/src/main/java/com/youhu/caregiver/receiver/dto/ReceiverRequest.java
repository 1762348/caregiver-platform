package com.youhu.caregiver.receiver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReceiverRequest(
        @NotBlank(message = "请输入姓名")
        @Size(max = 50, message = "姓名过长")
        String name,

        @NotBlank(message = "请输入身份证号")
        String idCard,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Size(max = 20, message = "关系描述过长")
        String relation,

        @Size(max = 1000, message = "健康状况描述过长")
        String health,

        @Size(max = 1000, message = "特殊需求描述过长")
        String special) {
}
