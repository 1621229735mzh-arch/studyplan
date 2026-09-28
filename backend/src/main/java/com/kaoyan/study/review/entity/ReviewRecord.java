package com.kaoyan.study.review.entity;

import java.time.LocalDate;

/** 复习记录：一次复习的反馈与据此得到的下次日期。 */
public class ReviewRecord {

    private Long id;
    private Long reviewItemId;
    private LocalDate reviewDate;
    private MasteryResult result;
    private Integer durationMinutes;
    private String note;
    private LocalDate nextReviewDate;
    private Integer intervalDays;
    private String clientToken;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewItemId() {
        return reviewItemId;
    }

    public void setReviewItemId(Long reviewItemId) {
        this.reviewItemId = reviewItemId;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public MasteryResult getResult() {
        return result;
    }

    public void setResult(MasteryResult result) {
        this.result = result;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public String getClientToken() {
        return clientToken;
    }

    public void setClientToken(String clientToken) {
        this.clientToken = clientToken;
    }
}
