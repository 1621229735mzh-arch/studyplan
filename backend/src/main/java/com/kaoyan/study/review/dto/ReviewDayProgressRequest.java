package com.kaoyan.study.review.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import com.kaoyan.study.review.entity.MasteryResult;
public record ReviewDayProgressRequest(
    @NotNull Long reviewItemId, @NotNull LocalDate date,
    @NotNull @Min(0) @Max(100) Integer completionPercent,
    @NotNull @Min(0) Integer durationMinutes, @NotNull Long version,
    MasteryResult result, @NotBlank @Size(max=64) String clientToken) {}
