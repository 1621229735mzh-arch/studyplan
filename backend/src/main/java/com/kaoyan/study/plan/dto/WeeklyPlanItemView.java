package com.kaoyan.study.plan.dto;

import java.math.BigDecimal;

/** 周计划中一条任务安排，附带该周已完成量。 */
public record WeeklyPlanItemView(
        Long taskId,
        String taskTitle,
        Long subjectId,
        Long unitId,
        String unitName,
        BigDecimal plannedAmount,
        BigDecimal completedAmount) {
}
