package com.kaoyan.study.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 同一天同一任务只统计一次实际完成量，多个排期来源合并展示。 */
public record ScheduleEntryView(LocalDate date, Long taskId, String taskTitle, Long subjectId,
        Long unitId, String unitName, BigDecimal plannedAmount, BigDecimal completedAmount,
        String source, Integer estimatedMinutes, Integer sortOrder) { }
