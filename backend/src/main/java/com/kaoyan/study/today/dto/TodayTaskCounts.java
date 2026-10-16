package com.kaoyan.study.today.dto;

/** 当天清单的互斥任务项数，不合计题、讲等学习量。 */
public record TodayTaskCounts(int completed, int inProgress, int remaining) {
}
