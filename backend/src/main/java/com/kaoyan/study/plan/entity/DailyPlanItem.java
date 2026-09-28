package com.kaoyan.study.plan.entity;

import java.math.BigDecimal;

/** 每日安排条目。 */
public class DailyPlanItem {

    private Long id;
    private java.time.LocalDate planDate;
    private Long taskId;
    private BigDecimal plannedAmount;
    /** 来源：手动安排、周计划下推、复习确认。 */
    private PlanSource source = PlanSource.MANUAL;
    private Long reviewItemId;
    private Long version = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public java.time.LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(java.time.LocalDate planDate) {
        this.planDate = planDate;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public PlanSource getSource() {
        return source;
    }

    public void setSource(PlanSource source) {
        this.source = source;
    }

    public Long getReviewItemId() {
        return reviewItemId;
    }

    public void setReviewItemId(Long reviewItemId) {
        this.reviewItemId = reviewItemId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
