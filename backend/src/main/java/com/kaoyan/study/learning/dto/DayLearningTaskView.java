package com.kaoyan.study.learning.dto;
import java.math.BigDecimal;
public record DayLearningTaskView(Long id, Long subjectId, String title, BigDecimal plannedAmount,
    BigDecimal completedAmount, BigDecimal completionPercent, Integer estimatedMinutes,
    BigDecimal remainingMinutes, String revision, com.kaoyan.study.learning.service.SequenceLearningService.State sequence,
    com.kaoyan.study.plan.service.TaskActivityService.Activity activity, String unitName) {}
