package com.kaoyan.study.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 外部 AI 的版本化交换合同。月、周说明不另存计划量；排期只来自 days。 */
public record PlanImportDocument(
        @NotNull Integer schemaVersion,
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String planId,
        @NotBlank @Size(max=200) String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Size(max=2000) List<@Valid Goal> goals,
        @NotNull @Size(max=1000) List<@Valid Month> months,
        @NotNull @Size(max=2000) List<@Valid Week> weeks,
        @NotNull @Size(min=1,max=20000) List<@Valid Task> tasks,
        @NotNull @Size(min=1,max=5000) List<@Valid Day> days) {
    public record Goal(@NotBlank @Size(max=64) String key, @NotBlank @Size(max=100) String subject,
            @NotBlank @Size(max=200) String title, @Size(max=1000) String description,
            @NotNull LocalDate startDate,@NotNull LocalDate endDate) { }
    public record Month(@NotBlank @Pattern(regexp="\\d{4}-\\d{2}") String month,
            @NotBlank @Size(max=200) String title,@Size(max=1000) String description) { }
    public record Week(@NotNull LocalDate startDate,@NotBlank @Size(max=200) String title,
                       @Size(max=1000) String description) { }
    public record Task(@NotBlank @Size(max=64) String key,@NotBlank @Size(max=100) String subject,
            @NotBlank @Size(max=100) String unit,@NotBlank @Size(max=200) String title,
            @Size(max=64) String goalKey,
            @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal plannedAmount) { }
    public record Day(@NotNull LocalDate date,@NotNull @Size(max=2000) List<@Valid Item> items) { }
    public record Item(@NotBlank @Size(max=64) String taskKey,
            @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal amount,
            @Min(0) Integer estimatedMinutes) { }
}
