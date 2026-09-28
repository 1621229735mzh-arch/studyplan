package com.kaoyan.study.plan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 手动调整某一天的安排量。
 *
 * <p>调整只改当天的安排，不会把未完成内容自动挪到次日。
 */
public record DailyItemAdjustRequest(
        @NotNull(message = "请填写调整后的计划量")
        @DecimalMin(value = "0", message = "计划量不能为负数")
        BigDecimal plannedAmount,

        @NotNull(message = "缺少数据版本，无法安全调整")
        Long version,

        @Size(max = 500, message = "原因过长")
        String reason) {
}
