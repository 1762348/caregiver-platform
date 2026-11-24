package com.youhu.caregiver.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** 分页响应，避免直接序列化 Spring Data Page 的冗余字段 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(),
                page.getNumber(), page.getSize());
    }
}
