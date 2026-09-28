package com.kaoyan.study.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 新建阶段目标。 */
public record StageGoalCreateRequest(
        @NotNull(message = "请选择科目")
        Long subjectId,

        @NotBlank(message = "请填写目标标题")
        @Size(max = 200, message = "目标标题过长")
        String title,

        @Size(max = 1000, message = "说明过长")
        String description,

        LocalDate startDate,

        LocalDate targetDate) {
}
