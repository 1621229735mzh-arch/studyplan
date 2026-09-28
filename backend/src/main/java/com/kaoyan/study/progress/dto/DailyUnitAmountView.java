package com.kaoyan.study.progress.dto;

import java.math.BigDecimal;

/** 趋势中某一天、某个单位的完成量。 */
public record DailyUnitAmountView(Long unitId, String unitName, BigDecimal amount) {
}
