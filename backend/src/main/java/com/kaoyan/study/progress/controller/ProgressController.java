package com.kaoyan.study.progress.controller;

import com.kaoyan.study.progress.dto.ProgressOverviewResponse;
import com.kaoyan.study.progress.dto.TrendPointView;
import com.kaoyan.study.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 进度看板：各科计划与实际对比、学习量与时长趋势。 */
@RestController
@RequestMapping("/api/progress")
@Tag(name = "进度")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/overview")
    @Operation(summary = "进度概览（按科目+单位分组，含趋势）")
    public ProgressOverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progressService.overview(from, to);
    }

    @GetMapping("/trend")
    @Operation(summary = "学习量与时长趋势（单位分别成列）")
    public List<TrendPointView> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progressService.trend(from, to);
    }
}
