package com.kaoyan.study.today.controller;

import com.kaoyan.study.today.dto.TodayResponse;
import com.kaoyan.study.today.service.TodayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 今日安排与记录入口。 */
@RestController
@RequestMapping("/api/today")
@Tag(name = "今日")
public class TodayController {

    private final TodayService todayService;

    public TodayController(TodayService todayService) {
        this.todayService = todayService;
    }

    @GetMapping
    @Operation(summary = "今日安排、今日记录与已确认的复习任务")
    public TodayResponse today(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return todayService.today(date);
    }
}
