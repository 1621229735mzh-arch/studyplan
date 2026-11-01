package com.kaoyan.study.review.dto;

import java.time.LocalDate;

/** A read-only execution recommendation; the original due date is never moved by viewing today. */
public record AutomaticReviewContext(Long streamId, String title, LocalDate dueDate,
        long overdueDays, int round, boolean optional, int remainingCount) {}
