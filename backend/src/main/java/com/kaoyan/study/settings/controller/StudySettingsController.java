package com.kaoyan.study.settings.controller;

import com.kaoyan.study.settings.dto.StudySettingsRequest;
import com.kaoyan.study.settings.dto.StudySettingsResponse;
import com.kaoyan.study.settings.service.StudySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 考试时间、学习预算与复习额度设置。 */
@RestController
@RequestMapping("/api/settings/study")
@Tag(name = "设置-学习预算")
public class StudySettingsController {

    private final StudySettingsService studySettingsService;

    public StudySettingsController(StudySettingsService studySettingsService) {
        this.studySettingsService = studySettingsService;
    }

    @GetMapping
    @Operation(summary = "读取学习设置")
    public StudySettingsResponse get() {
        return StudySettingsResponse.from(studySettingsService.get());
    }

    @PutMapping
    @Operation(summary = "更新学习设置（需携带 version）")
    public StudySettingsResponse update(@Valid @RequestBody StudySettingsRequest request) {
        return StudySettingsResponse.from(studySettingsService.update(request));
    }
}
