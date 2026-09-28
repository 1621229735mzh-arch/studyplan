package com.kaoyan.study.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 新建/修改备忘录的请求。
 *
 * <p>{@code version} 必填：新建时前端传 0，服务端只在校验更新时使用，用于检测
 * 数据是否已被其他设备修改。
 */
public record MemoRequest(
        Long subjectId,

        @NotBlank(message = "请填写备忘录内容")
        @Size(max = 1000, message = "备忘录内容过长")
        String content,

        LocalDate dueDate,

        /** OPEN / DONE；为空表示新建用 OPEN、修改保持原状态。 */
        String status,

        @NotNull(message = "缺少数据版本，无法安全更新")
        Long version) {
}
