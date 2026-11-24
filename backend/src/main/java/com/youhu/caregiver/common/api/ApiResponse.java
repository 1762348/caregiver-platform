package com.youhu.caregiver.common.api;

/**
 * 统一 API 响应体。
 *
 * <p>约定：{@code code} 与 HTTP 状态码保持一致（200 成功、400 参数错误、401 未登录、
 * 403 无权限、404 不存在、409 冲突、500 服务端异常），前端拦截器据此统一处理。</p>
 *
 * @param code    业务状态码
 * @param message 提示信息
 * @param data    业务数据，可为 null
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
