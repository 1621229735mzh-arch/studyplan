package com.kaoyan.study.learning.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** One atomic submission; each completed problem remains an ordinary study record. */
public record CompletedProblemNumbersRequest(
        @NotNull LocalDate date, @NotNull Long taskId,
        @NotEmpty @Size(max=200) List<@NotNull @Min(1) Integer> problemNumbers,
        @NotBlank @Size(max=4000) String revision,
        @NotBlank @Size(max=64) String clientToken) {}
