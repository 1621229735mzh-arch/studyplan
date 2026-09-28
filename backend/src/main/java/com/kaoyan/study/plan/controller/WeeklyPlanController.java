package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.dto.WeeklyPlanItemRequest;
import com.kaoyan.study.plan.dto.WeeklyPlanResponse;
import com.kaoyan.study.plan.service.WeeklyPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 周计划：按周一日期读取与安排任务。 */
@RestController
@RequestMapping("/api/plan/weeks")
@Tag(name = "计划-周计划")
public class WeeklyPlanController {

    private final WeeklyPlanService weeklyPlanService;

    public WeeklyPlanController(WeeklyPlanService weeklyPlanService) {
        this.weeklyPlanService = weeklyPlanService;
    }

    @GetMapping("/{weekStart}")
    @Operation(summary = "读取某周计划（周一日期）")
    public WeeklyPlanResponse get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return weeklyPlanService.getOrCreate(weekStart);
    }

    @PutMapping("/{weekStart}/items")
    @Operation(summary = "安排任务到本周（同一任务重复安排时更新计划量）")
    public WeeklyPlanResponse addOrUpdateItem(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @Valid @RequestBody WeeklyPlanItemRequest request) {
        return weeklyPlanService.addOrUpdateItem(weekStart, request);
    }

    @DeleteMapping("/{weekStart}/items/{taskId}")
    @Operation(summary = "从本周移除任务")
    public WeeklyPlanResponse removeItem(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @PathVariable Long taskId) {
        return weeklyPlanService.removeItem(weekStart, taskId);
    }
}
