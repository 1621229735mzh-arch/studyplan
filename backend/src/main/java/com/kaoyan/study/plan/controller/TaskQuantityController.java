package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.service.TaskQuantityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan/tasks/{id}/quantity-mode")
public class TaskQuantityController {
    private final TaskQuantityService service;
    public TaskQuantityController(TaskQuantityService service) { this.service=service; }
    @GetMapping public TaskQuantityService.Preview preview(@PathVariable Long id) { return service.preview(id); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable Long id,@Valid @RequestBody TaskQuantityService.Confirm request) { service.confirm(id,request); }
}
