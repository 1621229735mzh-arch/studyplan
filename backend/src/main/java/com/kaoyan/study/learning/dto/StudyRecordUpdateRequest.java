package com.kaoyan.study.learning.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 修改学习记录。必须携带 version，避免覆盖其他设备上的修改。
 *
 * <p>与提交一致：可以不填完成量（只记时长），此时科目与单位可为空。
 */
public record StudyRecordUpdateRequest(
        Long taskId,

        Long subjectId,

        Long unitId,

        @NotNull(message = "请选择学习日期")
        LocalDate recordDate,

        @DecimalMin(value = "0.01", message = "完成量必须大于 0")
        BigDecimal amount,

        @Min(value = 0, message = "用时不能为负数")
        Integer durationMinutes,

        @Size(max = 500, message = "学习内容过长")
        String content,

        @Size(max = 1000, message = "复盘过长")
        String note,

        @NotNull(message = "缺少数据版本，无法安全修改")
        Long version) {
}
