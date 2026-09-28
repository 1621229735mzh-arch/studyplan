package com.kaoyan.study.plan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务：周计划与今日安排共同引用的事实。
 *
 * <p>计划量可以为空，表示尚未确定；计划量不等于完成量，完成量只来自学习记录。
 */
public class Task {

    private Long id;
    private Long subjectId;
    private Long stageGoalId;
    private String title;
    private Long unitId;
    private BigDecimal plannedAmount;
    private TaskStatus status = TaskStatus.ACTIVE;
    private Long version = 0L;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getStageGoalId() {
        return stageGoalId;
    }

    public void setStageGoalId(Long stageGoalId) {
        this.stageGoalId = stageGoalId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
