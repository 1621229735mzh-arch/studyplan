package com.kaoyan.study.learning.dto;

/** Synchronous in-transaction notification; listeners must retain the task/stream lock order. */
public record StudyTaskChanged(Long taskId) {}
