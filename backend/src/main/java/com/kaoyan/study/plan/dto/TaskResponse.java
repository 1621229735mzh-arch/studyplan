package com.kaoyan.study.plan.dto;

import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.entity.TaskStatus;

import java.math.BigDecimal;

/** 任务响应。计量单位随任务固定，完成量不在这里返回（见进度接口）。 */
public record TaskResponse(
        Long id,
        Long subjectId,
        Long stageGoalId,
        String title,
        Long unitId,
        BigDecimal plannedAmount,
        TaskStatus status,
        Long version) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getSubjectId(), task.getStageGoalId(), task.getTitle(),
                task.getUnitId(), task.getPlannedAmount(), task.getStatus(), task.getVersion());
    }
}
