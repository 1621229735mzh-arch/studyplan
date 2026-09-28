package com.kaoyan.study.plan.dto;

import com.kaoyan.study.plan.entity.PlanSource;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 向某一天加入一条安排。默认来源为手动安排。 */
public record DailyItemCreateRequest(
        @NotNull(message = "请选择任务")
        Long taskId,

        @NotNull(message = "请填写计划量")
        @DecimalMin(value = "0.01", message = "计划量必须大于 0")
        BigDecimal plannedAmount,

        PlanSource source) {

    public PlanSource sourceOrDefault() {
        return source == null ? PlanSource.MANUAL : source;
    }
}
