package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.service.DailyPracticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan/tasks/{id}/daily-practice")
public class DailyPracticeController {
    private final DailyPracticeService service;
    public DailyPracticeController(DailyPracticeService service) { this.service=service; }
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id,@Valid @RequestBody DailyPracticeService.Request request) {
        service.enable(id,request);
    }
}
