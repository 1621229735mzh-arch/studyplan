package com.kaoyan.study.progress.dto;

import java.math.BigDecimal;

/**
 * 科目下单个计量单位的计划与实际。
 *
 * <p>{@code plannedAmount} 为 null 表示该单位下没有设置计划量，而不是 0；
 * 不同单位的数量永远不在这里相加，因此不存在“总体完成百分比”。
 */
public record UnitProgressView(
        Long subjectId,
        String subjectName,
        Long unitId,
        String unitName,
        BigDecimal plannedAmount,
        BigDecimal completedAmount,
        Integer studyMinutes) {
}
