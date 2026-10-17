package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.service.ProblemNumberModeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan/tasks/{id}/problem-number-mode")
public class ProblemNumberModeController {
    private final ProblemNumberModeService service;
    public ProblemNumberModeController(ProblemNumberModeService service) { this.service=service; }
    @GetMapping public ProblemNumberModeService.Preview preview(@PathVariable Long id) { return service.preview(id); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable Long id,@Valid @RequestBody ProblemNumberModeService.Confirm request) { service.confirm(id,request); }
}
