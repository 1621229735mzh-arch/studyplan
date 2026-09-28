package com.kaoyan.study.settings.dto;

import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.service.ReviewPhase;

import java.time.LocalDate;

/**
 * 学习设置响应。
 *
 * <p>{@code dailyStudyMinutes} / {@code dailyReviewMinutes} 为 null 表示尚未确定预算，
 * 前端应提示补充，而不是当作 0 处理。
 */
public record StudySettingsResponse(
        LocalDate examDate,
        Integer dailyStudyMinutes,
        Integer dailyReviewMinutes,
        ReviewPhase reviewPhase,
        Integer reviewIntervalForgotDays,
        Integer reviewIntervalVagueDays,
        Integer reviewIntervalMasteredDays,
        Long version) {

    public static StudySettingsResponse from(StudySettings settings) {
        return new StudySettingsResponse(settings.getExamDate(), settings.getDailyStudyMinutes(),
                settings.getDailyReviewMinutes(), settings.getReviewPhase(),
                settings.getReviewIntervalForgotDays(), settings.getReviewIntervalVagueDays(),
                settings.getReviewIntervalMasteredDays(), settings.getVersion());
    }
}
