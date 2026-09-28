package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.dto.DailyItemAdjustRequest;
import com.kaoyan.study.plan.dto.DailyItemCreateRequest;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.service.DailyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 每日安排：今日页面与计划页面共用。 */
@RestController
@RequestMapping("/api/plan/days")
@Tag(name = "计划-每日安排")
public class DailyPlanController {

    private final DailyPlanService dailyPlanService;

    public DailyPlanController(DailyPlanService dailyPlanService) {
        this.dailyPlanService = dailyPlanService;
    }

    @GetMapping("/{date}")
    @Operation(summary = "读取某天的安排（含当天已完成量）")
    public List<DailyPlanItemView> list(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dailyPlanService.list(date);
    }

    @PostMapping("/{date}/items")
    @Operation(summary = "加入当天安排")
    public List<DailyPlanItemView> addItem(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DailyItemCreateRequest request) {
        return dailyPlanService.addItem(date, request);
    }

    @PutMapping("/{date}/items/{itemId}")
    @Operation(summary = "手动调整当天安排量（需携带 version，并记录调整）")
    public List<DailyPlanItemView> adjust(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long itemId,
            @Valid @RequestBody DailyItemAdjustRequest request) {
        return dailyPlanService.adjust(date, itemId, request);
    }

    @DeleteMapping("/{date}/items/{itemId}")
    @Operation(summary = "移除当天安排")
    public List<DailyPlanItemView> removeItem(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long itemId) {
        return dailyPlanService.removeItem(date, itemId);
    }
}
