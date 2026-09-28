package com.kaoyan.study.learning.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 提交学习记录。
 *
 * <p>三种合法形态：
 * <ul>
 *   <li>关联任务：科目与单位以任务为准，可省略；</li>
 *   <li>计划外且有完成量：必须自带科目与单位，否则无法计入任何单位的进度；</li>
 *   <li>只记时长：不填完成量，也不要求科目与单位——它计入学习时长，不计入完成量。</li>
 * </ul>
 *
 * <p>{@code clientToken} 用于重复提交防重。
 */
public record StudyRecordCreateRequest(
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

        @Size(max = 64, message = "提交令牌过长")
        String clientToken) {
}
