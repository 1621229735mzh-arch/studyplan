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
            @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal plannedAmount,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            @Valid Sequence sequence,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            @Valid Activity activity,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
            @Valid Continuation continuation) {
        public Task(String key,String subject,String unit,String title,String goalKey,BigDecimal plannedAmount,Sequence sequence,Activity activity) {
            this(key,subject,unit,title,goalKey,plannedAmount,sequence,activity,null);
        }
        public Task(String key,String subject,String unit,String title,String goalKey,BigDecimal plannedAmount,Sequence sequence) {
            this(key,subject,unit,title,goalKey,plannedAmount,sequence,null);
        }
        public Task(String key,String subject,String unit,String title,String goalKey,BigDecimal plannedAmount) {
            this(key,subject,unit,title,goalKey,plannedAmount,null);
        }
    }
    public record Continuation(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String streamKey,
            @NotBlank @Size(max=200) String title,@NotNull @Min(1) Integer position) {}
    public record Activity(@NotBlank @Pattern(regexp="TIMED_STUDY|RECAP|CHECK_ONLY|CONTINUED_STUDY") String mode,
                           @Size(max=1000) String description,
                           @NotNull @Min(0) @Max(1440) Integer flexibleMinutes) {}
    public record Sequence(@NotBlank @Pattern(regexp="COMPLETE_THEN_ADVANCE") String mode,
            @NotNull @Size(min=1,max=5000) List<@NotNull @Valid SequenceItem> items) {}
    public record SequenceItem(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String key,
            @NotBlank @Size(max=200) String title,
            @NotBlank @Size(max=1000) String url) {}
    public record Day(@NotNull LocalDate date,@NotNull @Size(max=2000) List<@Valid Item> items) { }
    public record Item(@NotBlank @Size(max=64) String taskKey,
            @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal amount,
            @Min(0) Integer estimatedMinutes) { }
}
