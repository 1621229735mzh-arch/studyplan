package com.kaoyan.study.memo.controller;

import com.kaoyan.study.memo.dto.MemoConvertRequest;
import com.kaoyan.study.memo.dto.MemoRequest;
import com.kaoyan.study.memo.dto.MemoResponse;
import com.kaoyan.study.memo.service.MemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/** 备忘录维护。 */
@RestController
@RequestMapping("/api/memo")
@Tag(name = "备忘录")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping
    @Operation(summary = "备忘录列表（可按状态、科目与关键字筛选）")
    public List<MemoResponse> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long subjectId,
                                   @RequestParam(required = false) String keyword) {
        return memoService.list(status, subjectId, keyword).stream().map(MemoResponse::from).toList();
    }

    @GetMapping("/due")
    @Operation(summary = "到期提醒（默认当天，含此前未完成的备忘录）")
    public List<MemoResponse> due(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return memoService.listDue(date).stream().map(MemoResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "备忘录详情")
    public MemoResponse get(@PathVariable Long id) {
        return MemoResponse.from(memoService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增备忘录")
    public MemoResponse create(@Valid @RequestBody MemoRequest request) {
        return MemoResponse.from(memoService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改备忘录（需要数据版本）")
    public MemoResponse update(@PathVariable Long id, @Valid @RequestBody MemoRequest request) {
        return MemoResponse.from(memoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除备忘录（不删除已转成的任务）")
    public void delete(@PathVariable Long id) {
        memoService.delete(id);
    }

    @PostMapping("/{id}/dismiss-reminder")
    @Operation(summary = "忽略本次到期提醒")
    public MemoResponse dismissReminder(@PathVariable Long id) {
        return MemoResponse.from(memoService.dismissReminder(id));
    }

    @PostMapping("/{id}/convert-to-task")
    @Operation(summary = "转为计划任务（任务由计划模块创建，备忘录原文保留）")
    public MemoResponse convertToTask(@PathVariable Long id, @Valid @RequestBody MemoConvertRequest request) {
        return MemoResponse.from(memoService.convertToTask(id, request));
    }
}
