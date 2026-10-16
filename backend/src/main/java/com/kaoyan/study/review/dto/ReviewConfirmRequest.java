package com.kaoyan.study.review.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 确认把建议排入某一天。
 *
 * <p>每条可以补充预计用时：缺少预计用时时无法判断是否在额度内，
 * 旧额度模式要求补齐；manualSelection 表示本人直接选择，不作时间额度承诺。
 */
public record ReviewConfirmRequest(
        @NotNull(message = "请选择安排日期")
        LocalDate date,

        @NotEmpty(message = "请选择要安排的复习内容")
        List<@Valid Item> items,
        boolean manualSelection) {

    public ReviewConfirmRequest(LocalDate date, List<Item> items) {
        this(date, items, false);
    }

    public record Item(
            @NotNull(message = "缺少复习项")
            Long reviewItemId,

            @Min(value = 1, message = "预计用时必须大于 0")
            Integer estimatedMinutes) {
    }
}
