package com.kaoyan.study.review.dto;

import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewRecord;

import java.time.LocalDate;

/**
 * 复习反馈结果。
 *
 * <p>{@code intervalConfigured=false} 表示设置里还没填该档位的间隔天数，
 * 因此 {@code nextReviewDate} 为空——这是“待定参数未确定”，不是建议为“无”。
 */
public record ReviewRecordResponse(
        Long id,
        Long reviewItemId,
        LocalDate reviewDate,
        MasteryResult result,
        Integer durationMinutes,
        String note,
        LocalDate nextReviewDate,
        Integer intervalDays,
        boolean intervalConfigured) {

    public static ReviewRecordResponse from(ReviewRecord record, boolean intervalConfigured) {
        return new ReviewRecordResponse(record.getId(), record.getReviewItemId(), record.getReviewDate(),
                record.getResult(), record.getDurationMinutes(), record.getNote(),
                record.getNextReviewDate(), record.getIntervalDays(), intervalConfigured);
    }
}
