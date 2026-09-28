package com.kaoyan.study.progress.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 进度概览。
 *
 * <p>按“科目 + 单位”分组返回，前端只负责展示；后端不生成跨单位的总完成度。
 */
public record ProgressOverviewResponse(
        LocalDate from,
        LocalDate to,
        List<UnitProgressView> items,
        List<TrendPointView> trend) {
}
