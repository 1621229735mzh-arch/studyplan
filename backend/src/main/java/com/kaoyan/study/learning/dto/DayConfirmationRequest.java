package com.kaoyan.study.learning.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** A date-specific confirmation, independent from learned quantity and time. */
public record DayConfirmationRequest(@NotNull LocalDate date, @NotNull Long taskId,
        @NotNull Boolean completed, @NotBlank @Size(max=4000) String revision,
        @NotBlank @Size(max=64) String clientToken) { }
