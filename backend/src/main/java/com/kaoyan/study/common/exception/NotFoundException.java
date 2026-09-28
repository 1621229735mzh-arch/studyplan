package com.kaoyan.study.common.exception;

/** 请求的资源不存在（HTTP 404）。 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
