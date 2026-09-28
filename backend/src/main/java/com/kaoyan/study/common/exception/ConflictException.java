package com.kaoyan.study.common.exception;

/**
 * 与当前数据状态冲突（HTTP 409），例如重复提交同一学习记录、
 * 或调整方案应用时发现数据已被其他设备修改。
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
