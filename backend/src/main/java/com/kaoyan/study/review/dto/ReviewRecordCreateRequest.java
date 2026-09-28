package com.kaoyan.study.review.dto;

import com.kaoyan.study.review.entity.MasteryResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 提交复习反馈。
 *
 * <p>{@code nextReviewDate} 可手动指定；不指定时按设置中的间隔推导，
 * 间隔未设置则不臆造日期，返回 null 并由前端提示补充。
 */
public record ReviewRecordCreateRequest(
        @NotNull(message = "请选择复习内容")
        Long reviewItemId,

        @NotNull(message = "请选择复习日期")
        LocalDate reviewDate,

        @NotNull(message = "请选择掌握情况")
        MasteryResult result,

        @Min(value = 0, message = "用时不能为负数")
        Integer durationMinutes,

        @Size(max = 500, message = "备注过长")
        String note,

        LocalDate nextReviewDate,

        @Size(max = 64, message = "提交令牌过长")
        String clientToken) {
}
