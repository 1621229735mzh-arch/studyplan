package com.kaoyan.study.plan.entity;

/** 任务状态。未完成的任务不会因为到期自动顺延，状态只反映本人对任务的处置。 */
public enum TaskStatus {
    ACTIVE,
    DONE,
    ARCHIVED
}
