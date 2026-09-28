package com.kaoyan.study.plan.dto;

import java.time.LocalDate;
import java.util.List;

/** 周计划视图：本周安排的任务及各自完成量。 */
public record WeeklyPlanResponse(
        LocalDate weekStartDate,
        String note,
        Long version,
        List<WeeklyPlanItemView> items) {
}
