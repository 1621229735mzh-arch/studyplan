package com.kaoyan.study.plan.dto;

import com.kaoyan.study.plan.entity.PlanSource;

import java.math.BigDecimal;

/** 每日安排条目，附带当日已完成量。 */
public record DailyPlanItemView(
        Long id,
        Long taskId,
        String taskTitle,
        Long subjectId,
        Long unitId,
        String unitName,
        BigDecimal plannedAmount,
        PlanSource source,
        Long reviewItemId,
        Long version,
        BigDecimal completedAmount,
        Integer estimatedMinutes) {
}
