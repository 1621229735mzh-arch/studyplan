package com.kaoyan.study.learning.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record DayProgressRequest(
    @NotNull LocalDate date, @NotNull Long taskId,
    @NotNull @Min(0) @Max(100) Integer completionPercent,
    @NotNull @Min(0) Integer durationMinutes,
    @NotBlank @Size(max=4000) String revision,
    @NotBlank @Size(max=64) String clientToken) {}
