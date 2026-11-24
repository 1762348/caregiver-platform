package com.youhu.caregiver.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okWrapsDataWithStatus200() {
        ApiResponse<String> response = ApiResponse.ok("hello");

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("操作成功");
        assertThat(response.data()).isEqualTo("hello");
    }

    @Test
    void okWithoutDataHasNullPayload() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data()).isNull();
    }

    @Test
    void failKeepsCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.fail(409, "名额已被占用");

        assertThat(response.code()).isEqualTo(409);
        assertThat(response.message()).isEqualTo("名额已被占用");
        assertThat(response.data()).isNull();
    }
}
