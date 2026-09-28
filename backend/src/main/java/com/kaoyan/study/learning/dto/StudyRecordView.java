package com.kaoyan.study.learning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 学习记录列表项：带上科目/单位/任务名称，便于直接展示。 */
public record StudyRecordView(
        Long id,
        Long taskId,
        String taskTitle,
        Long subjectId,
        String subjectName,
        Long unitId,
        String unitName,
        LocalDate recordDate,
        BigDecimal amount,
        Integer durationMinutes,
        String content,
        String note,
        Long version) {
}
