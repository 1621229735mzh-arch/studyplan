package com.kaoyan.study.progress.service;

import com.kaoyan.study.progress.dto.DailyUnitAmountView;
import com.kaoyan.study.progress.dto.ProgressOverviewResponse;
import com.kaoyan.study.progress.dto.TrendPointView;
import com.kaoyan.study.progress.dto.UnitProgressView;
import com.kaoyan.study.progress.mapper.ProgressMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 进度统计。
 *
 * <p>只读查询：完成量完全由学习记录推导，所以修改或删除记录后这里的数字会立刻变化。
 * 结果按“科目 + 单位”分组返回，后端不会给出跨单位相加的总完成度。
 */
@Service
public class ProgressService {

    /** 默认观察窗口：最近 4 周。 */
    private static final int DEFAULT_WINDOW_DAYS = 28;

    private final ProgressMapper progressMapper;

    public ProgressService(ProgressMapper progressMapper) {
        this.progressMapper = progressMapper;
    }

    public ProgressOverviewResponse overview(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_WINDOW_DAYS - 1L) : from;

        List<UnitProgressView> items = progressMapper.findUnitProgress(start, end);
        return new ProgressOverviewResponse(start, end, items, trend(start, end));
    }

    public List<TrendPointView> trend(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_WINDOW_DAYS - 1L) : from;

        Map<LocalDate, Integer> minutesByDate = new LinkedHashMap<>();
        for (ProgressMapper.DailyMinutesRow row : progressMapper.findDailyMinutes(start, end)) {
            minutesByDate.put(row.date(), row.studyMinutes() == null ? 0 : row.studyMinutes());
        }

        Map<LocalDate, List<DailyUnitAmountView>> amountsByDate = new LinkedHashMap<>();
        for (ProgressMapper.DailyUnitAmountRow row : progressMapper.findDailyUnitAmounts(start, end)) {
            amountsByDate.computeIfAbsent(row.date(), key -> new ArrayList<>())
                    .add(new DailyUnitAmountView(row.unitId(), row.unitName(), row.amount()));
        }

        // 合并两段查询，保证有记录的日期都出现在趋势里
        List<TrendPointView> points = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            int minutes = minutesByDate.getOrDefault(date, 0);
            List<DailyUnitAmountView> amounts = amountsByDate.getOrDefault(date, List.of());
            if (minutes > 0 || !amounts.isEmpty()) {
                points.add(new TrendPointView(date, minutes, amounts));
            }
        }
        return points;
    }
}
