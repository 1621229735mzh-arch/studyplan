package com.kaoyan.study.settings.dto;

import com.kaoyan.study.settings.service.ReviewPhase;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 更新学习设置的请求。
 *
 * <p>{@code version} 必填：多设备编辑时用于检测数据是否已被另一台设备修改。
 */
public record StudySettingsRequest(
        LocalDate examDate,

        @Min(value = 0, message = "每日学习预算不能为负数")
        Integer dailyStudyMinutes,

        @Min(value = 0, message = "每日复习额度不能为负数")
        Integer dailyReviewMinutes,

        ReviewPhase reviewPhase,

        @Min(value = 0, message = "复习间隔不能为负数")
        Integer reviewIntervalForgotDays,

        @Min(value = 0, message = "复习间隔不能为负数")
        Integer reviewIntervalVagueDays,

        @Min(value = 0, message = "复习间隔不能为负数")
        Integer reviewIntervalMasteredDays,

        @NotNull(message = "缺少数据版本，无法安全更新")
        Long version) {
}
