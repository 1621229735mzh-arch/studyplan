package com.kaoyan.study.settings.entity;

import com.kaoyan.study.settings.service.ReviewPhase;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习设置（单行）。
 *
 * <p>学习预算与复习额度是待定项，因此允许为空：为空表示尚未确定，
 * 业务层不得用臆造的默认值宣称满足额度。
 */
public class StudySettings {

    private Long id;
    private LocalDate examDate;
    private Integer dailyStudyMinutes;
    private Integer dailyReviewMinutes;
    private ReviewPhase reviewPhase = ReviewPhase.MAIN;
    private Integer reviewIntervalForgotDays;
    private Integer reviewIntervalVagueDays;
    private Integer reviewIntervalMasteredDays;
    private LocalDateTime updatedAt;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Integer getDailyStudyMinutes() {
        return dailyStudyMinutes;
    }

    public void setDailyStudyMinutes(Integer dailyStudyMinutes) {
        this.dailyStudyMinutes = dailyStudyMinutes;
    }

    public Integer getDailyReviewMinutes() {
        return dailyReviewMinutes;
    }

    public void setDailyReviewMinutes(Integer dailyReviewMinutes) {
        this.dailyReviewMinutes = dailyReviewMinutes;
    }

    public ReviewPhase getReviewPhase() {
        return reviewPhase;
    }

    public void setReviewPhase(ReviewPhase reviewPhase) {
        this.reviewPhase = reviewPhase;
    }

    public Integer getReviewIntervalForgotDays() {
        return reviewIntervalForgotDays;
    }

    public void setReviewIntervalForgotDays(Integer reviewIntervalForgotDays) {
        this.reviewIntervalForgotDays = reviewIntervalForgotDays;
    }

    public Integer getReviewIntervalVagueDays() {
        return reviewIntervalVagueDays;
    }

    public void setReviewIntervalVagueDays(Integer reviewIntervalVagueDays) {
        this.reviewIntervalVagueDays = reviewIntervalVagueDays;
    }

    public Integer getReviewIntervalMasteredDays() {
        return reviewIntervalMasteredDays;
    }

    public void setReviewIntervalMasteredDays(Integer reviewIntervalMasteredDays) {
        this.reviewIntervalMasteredDays = reviewIntervalMasteredDays;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
