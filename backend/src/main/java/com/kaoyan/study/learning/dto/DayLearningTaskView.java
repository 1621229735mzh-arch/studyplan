package com.kaoyan.study.learning.dto;
import java.math.BigDecimal;
public record DayLearningTaskView(Long id, Long subjectId, String title, BigDecimal plannedAmount,
    BigDecimal completedAmount, BigDecimal completionPercent, Integer estimatedMinutes,
    BigDecimal remainingMinutes, String revision) {}
