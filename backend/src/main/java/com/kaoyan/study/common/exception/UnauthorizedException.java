package com.kaoyan.study.common.exception;

/** 未认证或认证失败（HTTP 401）。 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}
