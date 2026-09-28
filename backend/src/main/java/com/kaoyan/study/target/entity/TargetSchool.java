package com.kaoyan.study.target.entity;

import java.time.LocalDateTime;

/**
 * 候选目标院校：本人考虑的报考目标，与 plan 的阶段目标不是同一事实。
 *
 * <p>不直接用于接口响应：接口只暴露 {@code target.dto} 中的类型。
 */
public class TargetSchool {

    private Long id;
    private String schoolName;
    private String majorName;
    private String note;
    /** CANDIDATE 候选 / CHOSEN 已定 / DROPPED 放弃。 */
    private String status = "CANDIDATE";
    private int sortOrder;
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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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
