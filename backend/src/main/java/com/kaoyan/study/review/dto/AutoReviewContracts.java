package com.kaoyan.study.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public final class AutoReviewContracts {
    private AutoReviewContracts() {}
    public record PolicyRequest(@NotNull Boolean enabled, @NotNull @Min(0) Long version) {}
    public record Book(Long streamId, Long subjectId, String title, boolean enabled, Long version,
                       int segmentCount, int reviewCount, int overdueCount, int suspendedCount) {}
    public record State(Long reviewItemId, Long streamId, LocalDate completedOn, boolean sourceValid,
                        boolean paused, Double stability, Double difficulty, LocalDate lastReviewDate,
                        int repetitions, String algorithm) {}
    public record Segment(Long reviewItemId, Long streamId, String title, LocalDate completedOn,
                          boolean sourceValid, boolean paused, boolean enabled, String status,
                          LocalDate scheduledDate, LocalDate nextReviewDate, int repetitions, Long version, String dateSource) {}
}
