package com.kaoyan.study.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** date 为空表示移除待执行安排，历史复习记录保持。 */
public record ReviewScheduleRequest(@NotNull Long reviewItemId, LocalDate date,
                                    @NotNull @PositiveOrZero Long version) {}
