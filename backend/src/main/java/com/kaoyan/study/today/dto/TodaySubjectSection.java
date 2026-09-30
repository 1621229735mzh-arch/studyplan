package com.kaoyan.study.today.dto;
import java.math.BigDecimal;
import java.util.List;
public record TodaySubjectSection(String name, int actualMinutes, BigDecimal remainingMinutes,
    int missingEstimates, int missingDurations, List<TodayChecklistTask> tasks) {}
