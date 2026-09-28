package com.kaoyan.study.common.exception;

/**
 * 业务规则被违反时抛出的异常，由 {@link GlobalExceptionHandler} 统一转换为接口错误响应。
 *
 * <p>不用于替代参数校验：请求格式错误由 Bean Validation 处理。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
