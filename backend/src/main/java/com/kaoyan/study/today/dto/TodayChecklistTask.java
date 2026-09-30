package com.kaoyan.study.today.dto;
import java.math.BigDecimal;
public record TodayChecklistTask(String kind, Long id, String title, BigDecimal completionPercent,
    Integer estimatedMinutes, BigDecimal remainingMinutes, String revision, Long version, boolean actionable) {}
