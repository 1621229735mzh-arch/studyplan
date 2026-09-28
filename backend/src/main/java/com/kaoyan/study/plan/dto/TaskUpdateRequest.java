package com.kaoyan.study.plan.dto;

import com.kaoyan.study.plan.entity.TaskStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 修改任务。必须携带 version，避免覆盖其他设备上的修改。 */
public record TaskUpdateRequest(
        @NotNull(message = "请选择科目")
        Long subjectId,

        Long stageGoalId,

        @NotBlank(message = "请填写任务标题")
        @Size(max = 200, message = "任务标题过长")
        String title,

        @NotNull(message = "请选择计量单位")
        Long unitId,

        @DecimalMin(value = "0", message = "计划量不能为负数")
        BigDecimal plannedAmount,

        TaskStatus status,

        @NotNull(message = "缺少数据版本，无法安全更新")
        Long version) {
}
