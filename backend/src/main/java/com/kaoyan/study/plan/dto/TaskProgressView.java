package com.kaoyan.study.plan.dto;

import java.math.BigDecimal;

/**
 * 单个任务的计划与实际对比。
 *
 * <p>{@code remainingAmount} 由 SQL 计算：计划量未确定时为 null，
 * 不臆造“剩余工作量”与完成百分比。不同单位的数量不在这里相加。
 */
public record TaskProgressView(
        Long taskId,
        String title,
        Long subjectId,
        Long unitId,
        String unitName,
        BigDecimal plannedAmount,
        BigDecimal completedAmount,
        BigDecimal remainingAmount) {
}
