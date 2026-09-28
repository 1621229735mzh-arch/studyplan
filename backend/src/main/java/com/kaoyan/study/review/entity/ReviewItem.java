package com.kaoyan.study.review.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 复习项：只能基于已经学过并主动加入复习的内容。 */
public class ReviewItem {

    private Long id;
    private Long subjectId;
    private Long taskId;
    private String title;
    private Long sourceRecordId;
    private ReviewStatus status = ReviewStatus.PENDING;
    private LocalDate nextReviewDate;
    private MasteryResult lastResult;
    private Integer intervalDays;
    /** 预计用时；为空时无法判断是否符合额度，确认安排前需要补充。 */
    private Integer estimatedMinutes;
    private LocalDate scheduledDate;
    private LocalDateTime addedAt;
    private Long version = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(Long sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDate nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public MasteryResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(MasteryResult lastResult) {
        this.lastResult = lastResult;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
