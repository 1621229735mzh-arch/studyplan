package com.kaoyan.study.review.controller;

import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.service.ReviewWorkspaceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review/workspace")
public class ReviewWorkspaceController {
    private final ReviewWorkspaceService service;
    public ReviewWorkspaceController(ReviewWorkspaceService service) { this.service = service; }
    @GetMapping
    public ReviewWorkspaceResponse workspace(@RequestParam(required = false) Long subjectId) {
        return service.workspace(subjectId);
    }
    @PostMapping("/schedule")
    public ReviewItemResponse schedule(@Valid @RequestBody ReviewScheduleRequest request) {
        return service.schedule(request);
    }
}
