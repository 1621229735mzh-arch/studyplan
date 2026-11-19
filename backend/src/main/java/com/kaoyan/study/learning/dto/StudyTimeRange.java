package com.kaoyan.study.learning.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record StudyTimeRange(@NotBlank String revision, @NotNull LocalDateTime endedAt) {}
