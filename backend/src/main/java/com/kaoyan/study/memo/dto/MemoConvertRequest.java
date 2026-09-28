package com.kaoyan.study.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 把备忘录转成计划任务的请求。
 *
 * <p>任务标题与科目/单位由本人确认后提交，不由备忘录内容自动推断：
 * 备忘录是随手记，缺少单位与科目时不能直接变成计划量。
 */
public record MemoConvertRequest(
        @NotBlank(message = "请填写任务标题")
        @Size(max = 200, message = "任务标题过长")
        String taskTitle,

        @NotNull(message = "请选择科目")
        Long subjectId,

        @NotNull(message = "请选择计量单位")
        Long unitId,

        /** 计划量可以为空，表示尚未确定；为空不视为 0。 */
        BigDecimal plannedAmount) {
}
