package com.kaoyan.study.target.controller;

import com.kaoyan.study.target.dto.TargetRequest;
import com.kaoyan.study.target.dto.TargetResponse;
import com.kaoyan.study.target.dto.TargetStatusRequest;
import com.kaoyan.study.target.service.TargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 候选目标维护。 */
@RestController
@RequestMapping("/api/targets")
@Tag(name = "候选目标")
public class TargetController {

    private final TargetService targetService;

    public TargetController(TargetService targetService) {
        this.targetService = targetService;
    }

    @GetMapping
    @Operation(summary = "候选目标列表（可按状态筛选）")
    public List<TargetResponse> list(@RequestParam(required = false) String status) {
        return targetService.list(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "候选目标详情（含参考链接）")
    public TargetResponse get(@PathVariable Long id) {
        return targetService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增候选目标")
    public TargetResponse create(@Valid @RequestBody TargetRequest request) {
        return targetService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改候选目标（需要数据版本，链接整体替换）")
    public TargetResponse update(@PathVariable Long id, @Valid @RequestBody TargetRequest request) {
        return targetService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改候选目标状态（需要数据版本）")
    public TargetResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TargetStatusRequest request) {
        return targetService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除候选目标（链接一并删除）")
    public void delete(@PathVariable Long id) {
        targetService.delete(id);
    }
}
