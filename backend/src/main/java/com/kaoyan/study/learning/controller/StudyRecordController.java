package com.kaoyan.study.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.kaoyan.study.learning.dto.DayProgressRequest;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordUpdateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;

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

/** 学习记录：提交、补记、修改与删除。 */
@RestController
@RequestMapping("/api/learning/records")
@Tag(name = "学习记录")
public class StudyRecordController {

    private final StudyRecordService studyRecordService;

    public StudyRecordController(StudyRecordService studyRecordService) {
        this.studyRecordService = studyRecordService;
    }

    @GetMapping
    @Operation(summary = "学习记录列表（按日期区间、任务或科目筛选）")
    public List<StudyRecordView> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long subjectId) {
        return studyRecordService.list(from, to, taskId, subjectId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "学习记录详情")
    public StudyRecordView get(@PathVariable Long id) {
        return studyRecordService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "提交学习记录（带 clientToken 时重复提交不会生成第二条）")
    public StudyRecordView create(@Valid @RequestBody StudyRecordCreateRequest request) {
        return studyRecordService.create(request);
    }

    @PostMapping("/day-progress")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyRecordView dayProgress(@Valid @RequestBody DayProgressRequest request) {
        return studyRecordService.recordDayProgress(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改学习记录（需携带 version）")
    public StudyRecordView update(@PathVariable Long id, @Valid @RequestBody StudyRecordUpdateRequest request) {
        return studyRecordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除学习记录（软删除，统计随之重算）")
    public void delete(@PathVariable Long id, @RequestParam Long version) {
        studyRecordService.delete(id, version);
    }
}
