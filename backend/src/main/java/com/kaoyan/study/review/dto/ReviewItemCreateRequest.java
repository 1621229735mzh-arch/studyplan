package com.kaoyan.study.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 把已学内容加入复习。
 *
 * <p>关联任务时科目与标题取任务信息；计划外内容必须关联有效学习记录，科目取记录。
 */
public record ReviewItemCreateRequest(
        Long taskId,

        Long subjectId,

        @Size(max = 200, message = "标题过长")
        String title,

        @Min(value = 1, message = "预计用时必须大于 0")
        Integer estimatedMinutes,

        Long sourceRecordId) {

    public ReviewItemCreateRequest(Long taskId, Long subjectId, String title, Integer estimatedMinutes) {
        this(taskId, subjectId, title, estimatedMinutes, null);
    }
}
