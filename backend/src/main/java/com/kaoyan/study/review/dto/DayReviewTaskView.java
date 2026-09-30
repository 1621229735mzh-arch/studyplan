package com.kaoyan.study.review.dto;
import java.math.BigDecimal;
public record DayReviewTaskView(Long id, Long subjectId, String title, BigDecimal completionPercent,
    Integer estimatedMinutes, Integer actualMinutes, Integer missingDurations, Long version, boolean actionable) {}
