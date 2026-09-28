package com.kaoyan.study.settings.controller;

import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.dto.SubjectResponse;
import com.kaoyan.study.settings.service.SubjectService;
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

/** 科目维护。 */
@RestController
@RequestMapping("/api/settings/subjects")
@Tag(name = "设置-科目")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    @Operation(summary = "科目列表")
    public List<SubjectResponse> list(@RequestParam(defaultValue = "false") boolean enabledOnly) {
        return subjectService.list(enabledOnly).stream().map(SubjectResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新增科目")
    public SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return SubjectResponse.from(subjectService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改科目")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return SubjectResponse.from(subjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除科目（被任务引用时拒绝）")
    public void delete(@PathVariable Long id) {
        subjectService.delete(id);
    }
}
