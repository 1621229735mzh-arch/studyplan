package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.dto.TaskProgressView;
import com.kaoyan.study.plan.dto.TaskResponse;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 任务维护与任务级进度。 */
@RestController
@RequestMapping("/api/plan/tasks")
@Tag(name = "计划-任务")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "任务列表")
    public List<TaskResponse> list(@RequestParam(required = false) Long subjectId,
                                   @RequestParam(required = false) Long stageGoalId,
                                   @RequestParam(required = false) String status) {
        return taskService.list(subjectId, stageGoalId, status).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public TaskResponse get(@PathVariable Long id) {
        return TaskResponse.from(taskService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新建任务")
    public TaskResponse create(@Valid @RequestBody TaskCreateRequest request) {
        return TaskResponse.from(taskService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改任务（需携带 version）")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return TaskResponse.from(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除任务（有学习记录时拒绝）")
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "任务计划与实际对比（完成量来自学习记录）")
    public TaskProgressView progress(@PathVariable Long id) {
        return taskService.progress(id);
    }
}
