package com.kaoyan.study.common.api;

import java.time.Instant;
import java.util.List;

/**
 * 统一的接口错误响应。
 *
 * @param code       稳定的机器可读错误码
 * @param message    面向使用者的中文说明
 * @param path       出错请求路径
 * @param timestamp  服务端时间
 * @param fieldErrors 参数校验失败的字段明细，可为空列表
 */
public record ApiError(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> fieldErrors) {

    /** 单个字段的校验失败信息。 */
    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, path, Instant.now(), List.of());
    }

    public static ApiError of(String code, String message, String path, List<FieldViolation> fieldErrors) {
        return new ApiError(code, message, path, Instant.now(), List.copyOf(fieldErrors));
    }
}
