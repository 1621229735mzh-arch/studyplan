package com.kaoyan.study.review.dto;

public record QueuedReviewTask(DayReviewTaskView task, AutomaticReviewContext automaticReview) {}
