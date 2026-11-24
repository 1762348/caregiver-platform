package com.youhu.caregiver.common.exception;

import lombok.Getter;

/**
 * 可预期的业务异常：由全局异常处理器转换为对应 HTTP 状态码与提示信息。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }
}
