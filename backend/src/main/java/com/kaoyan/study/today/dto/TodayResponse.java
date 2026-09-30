package com.kaoyan.study.today.dto;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.review.dto.ReviewItemResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 今日视图：今天做什么、完成多少。
 *
 * <p>学习时长、内容完成量、复习安排分别呈现，不做跨单位合计；
 * 未设置预算或额度时通过 {@code warnings} 明确说明，不宣称满足额度。
 */
public record TodayResponse(
        LocalDate date,
        List<DailyPlanItemView> plannedTasks,
        List<StudyRecordView> records,
        List<ReviewItemResponse> reviewTasks,
        Integer dailyStudyMinutes,
        Integer dailyReviewMinutes,
        int studyMinutes,
        List<String> warnings,
        int actualMinutes, java.math.BigDecimal remainingMinutes, int missingEstimates, int missingDurations,
        List<TodaySubjectSection> sections) {
}
