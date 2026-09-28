package com.kaoyan.study.review.dto;

import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewItem;
import com.kaoyan.study.review.entity.ReviewStatus;

import java.time.LocalDate;

/** 复习项响应。 */
public record ReviewItemResponse(
        Long id,
        Long subjectId,
        Long taskId,
        String title,
        ReviewStatus status,
        LocalDate nextReviewDate,
        MasteryResult lastResult,
        Integer intervalDays,
        Integer estimatedMinutes,
        LocalDate scheduledDate,
        Long version) {

    public static ReviewItemResponse from(ReviewItem item) {
        return new ReviewItemResponse(item.getId(), item.getSubjectId(), item.getTaskId(), item.getTitle(),
                item.getStatus(), item.getNextReviewDate(), item.getLastResult(), item.getIntervalDays(),
                item.getEstimatedMinutes(), item.getScheduledDate(), item.getVersion());
    }
}
