package com.kaoyan.study.plan.dto;

import com.kaoyan.study.plan.entity.StageGoal;

import java.time.LocalDate;

/** 阶段目标响应。 */
public record StageGoalResponse(
        Long id,
        Long subjectId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate targetDate,
        String status,
        Long version) {

    public static StageGoalResponse from(StageGoal goal) {
        return new StageGoalResponse(goal.getId(), goal.getSubjectId(), goal.getTitle(), goal.getDescription(),
                goal.getStartDate(), goal.getTargetDate(), goal.getStatus(), goal.getVersion());
    }
}
