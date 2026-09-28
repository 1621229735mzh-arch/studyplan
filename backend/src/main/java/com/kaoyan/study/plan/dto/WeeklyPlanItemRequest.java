package com.kaoyan.study.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 周计划中安排一条任务。 */
public record WeeklyPlanItemRequest(
        @NotNull(message = "请选择任务")
        Long taskId,

        @NotNull(message = "请填写计划量")
        @DecimalMin(value = "0.01", message = "计划量必须大于 0")
        BigDecimal plannedAmount) {
}
