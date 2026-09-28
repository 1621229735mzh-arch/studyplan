package com.kaoyan.study.progress.dto;

import java.time.LocalDate;
import java.util.List;

/** 某一天的趋势点：时长与各单位完成量分开呈现，不做跨单位合计。 */
public record TrendPointView(LocalDate date, int studyMinutes, List<DailyUnitAmountView> amounts) {
}
