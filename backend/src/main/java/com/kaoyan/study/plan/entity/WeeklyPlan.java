package com.kaoyan.study.plan.entity;

import java.time.LocalDate;

/** 周计划（一周一条，以周一日期标识）。 */
public class WeeklyPlan {

    private Long id;
    private LocalDate weekStartDate;
    private String note;
    private Long version = 0L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
