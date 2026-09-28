package com.kaoyan.study.review.entity;

/** 复习项状态。待安排的内容会一直保留到期信息，不会自动挤占新学习时间。 */
public enum ReviewStatus {

    /** 待安排：已加入复习但尚未确认进入某一天。 */
    PENDING,

    /** 已确认安排到某一天。 */
    SCHEDULED,

    /** 归档：不再复习。 */
    ARCHIVED
}
