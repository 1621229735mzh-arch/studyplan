package com.kaoyan.study.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlanWorkspaceResponse(long revision, List<Month> months, List<ScheduleEntryView> entries,
        List<Outline> outlines, List<ImportedPlan> imports) {
    public record Amount(Long subjectId, Long unitId, String unitName,
                         BigDecimal plannedAmount, BigDecimal completedAmount) { }
    public record Month(String month, int scheduledDays, int taskCount, List<Amount> amounts) { }
    public record Outline(String periodType, LocalDate startDate, String title, String description) { }
    public record ImportedPlan(String planKey, String title, LocalDate startDate, LocalDate endDate) { }
}
