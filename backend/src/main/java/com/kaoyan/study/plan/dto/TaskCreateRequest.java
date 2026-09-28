package com.kaoyan.study.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 新建任务。计划量可以留空，表示尚未确定。 */
public record TaskCreateRequest(
        @NotNull(message = "请选择科目")
        Long subjectId,

        Long stageGoalId,

        @NotBlank(message = "请填写任务标题")
        @Size(max = 200, message = "任务标题过长")
        String title,

        @NotNull(message = "请选择计量单位")
        Long unitId,

        @DecimalMin(value = "0", message = "计划量不能为负数")
        BigDecimal plannedAmount) {
}
