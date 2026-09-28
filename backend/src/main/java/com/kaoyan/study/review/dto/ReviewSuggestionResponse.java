package com.kaoyan.study.review.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 某一天的复习建议。
 *
 * <p>{@code dailyReviewMinutes} 为空表示本人尚未设置复习额度，此时
 * {@code remainingMinutes} 也为空，前端不得宣称建议符合额度。
 * {@code warnings} 明确列出缺少的输入（额度、预计用时）。
 */
public record ReviewSuggestionResponse(
        LocalDate date,
        Integer dailyReviewMinutes,
        int scheduledMinutes,
        Integer remainingMinutes,
        List<Item> items,
        List<String> warnings) {

    /** 单条建议：到期日保留，积压时以 overdueDays 体现，但不自动提高强度。 */
    public record Item(
            Long reviewItemId,
            String title,
            Long subjectId,
            LocalDate dueDate,
            long overdueDays,
            Integer estimatedMinutes) {
    }
}
