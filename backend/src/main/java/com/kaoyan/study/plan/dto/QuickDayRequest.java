package com.kaoyan.study.plan.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record QuickDayRequest(Long existingTaskId, Long subjectId, Long unitId,
        @Size(max=200) String title,
        @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal plannedAmount,
        @Min(0) Integer estimatedMinutes,
        @NotBlank @Size(max=64) String clientToken) { }
