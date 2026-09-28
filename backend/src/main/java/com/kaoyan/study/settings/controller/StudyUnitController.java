package com.kaoyan.study.settings.controller;

import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.dto.StudyUnitResponse;
import com.kaoyan.study.settings.service.StudyUnitService;
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

/** 计量单位维护。 */
@RestController
@RequestMapping("/api/settings/units")
@Tag(name = "设置-计量单位")
public class StudyUnitController {

    private final StudyUnitService studyUnitService;

    public StudyUnitController(StudyUnitService studyUnitService) {
        this.studyUnitService = studyUnitService;
    }

    @GetMapping
    @Operation(summary = "单位列表")
    public List<StudyUnitResponse> list(@RequestParam(defaultValue = "false") boolean enabledOnly) {
        return studyUnitService.list(enabledOnly).stream().map(StudyUnitResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增单位")
    public StudyUnitResponse create(@Valid @RequestBody StudyUnitRequest request) {
        return StudyUnitResponse.from(studyUnitService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改单位")
    public StudyUnitResponse update(@PathVariable Long id, @Valid @RequestBody StudyUnitRequest request) {
        return StudyUnitResponse.from(studyUnitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除单位（被引用时拒绝）")
    public void delete(@PathVariable Long id) {
        studyUnitService.delete(id);
    }
}
