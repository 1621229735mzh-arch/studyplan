package com.kaoyan.study.review.dto;

import com.kaoyan.study.review.entity.MasteryResult;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class ReviewFeedbackCorrection {
    private ReviewFeedbackCorrection() {}
    public record Request(@NotNull @Positive Long recordId, @NotNull @Min(0) Long version,
                          @NotNull MasteryResult result, @NotBlank @Size(max=80) String clientToken) {}
    public record Context(Long version, ReviewRecordResponse record, boolean allowed, String reason,
                          LocalDate currentNextReviewDate, LocalDate scheduledDate) {}
    public record Result(Long reviewItemId, Long recordId, MasteryResult result, LocalDate nextReviewDate,
                         LocalDate scheduledDate, Integer intervalDays, Long version,
                         @com.fasterxml.jackson.annotation.JsonIgnore String requestHash) {}
}
