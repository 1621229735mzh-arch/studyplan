package com.kaoyan.study.memo.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 备忘录：随手记的事实，可关联科目、设置到期提醒，并在本人确认后转成计划任务。
 *
 * <p>不直接用于接口响应：接口只暴露 {@code memo.dto} 中的类型。
 */
public class Memo {

    private Long id;
    private Long subjectId;
    private String content;
    /** 到期日期，用于站内提醒；为空表示不提醒。 */
    private LocalDate dueDate;
    /** OPEN / DONE。 */
    private String status = "OPEN";
    /** 转成的任务；只记录来源关系，任务本身归 plan 模块维护。 */
    private Long convertedTaskId;
    /** 是否已忽略本次到期提醒；忽略只影响提醒展示，不改变内容与状态。 */
    private boolean reminderDismissed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 数据版本，用于多设备编辑检查。 */
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getConvertedTaskId() {
        return convertedTaskId;
    }

    public void setConvertedTaskId(Long convertedTaskId) {
        this.convertedTaskId = convertedTaskId;
    }

    public boolean isReminderDismissed() {
        return reminderDismissed;
    }

    public void setReminderDismissed(boolean reminderDismissed) {
        this.reminderDismissed = reminderDismissed;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
