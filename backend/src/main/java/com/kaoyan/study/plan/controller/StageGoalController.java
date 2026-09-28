package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.dto.StageGoalCreateRequest;
import com.kaoyan.study.plan.dto.StageGoalResponse;
import com.kaoyan.study.plan.dto.StageGoalUpdateRequest;
import com.kaoyan.study.plan.service.StageGoalService;
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

/** 阶段目标维护。 */
@RestController
@RequestMapping("/api/plan/stage-goals")
@Tag(name = "计划-阶段目标")
public class StageGoalController {

    private final StageGoalService stageGoalService;

    public StageGoalController(StageGoalService stageGoalService) {
        this.stageGoalService = stageGoalService;
    }

    @GetMapping
    @Operation(summary = "阶段目标列表")
    public List<StageGoalResponse> list(@RequestParam(required = false) Long subjectId,
                                        @RequestParam(required = false) String status) {
        return stageGoalService.list(subjectId, status).stream().map(StageGoalResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "阶段目标详情")
    public StageGoalResponse get(@PathVariable Long id) {
        return StageGoalResponse.from(stageGoalService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新建阶段目标")
    public StageGoalResponse create(@Valid @RequestBody StageGoalCreateRequest request) {
        return StageGoalResponse.from(stageGoalService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改阶段目标（需携带 version）")
    public StageGoalResponse update(@PathVariable Long id, @Valid @RequestBody StageGoalUpdateRequest request) {
        return StageGoalResponse.from(stageGoalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除阶段目标（已被任务引用时拒绝）")
    public void delete(@PathVariable Long id) {
        stageGoalService.delete(id);
    }
}
