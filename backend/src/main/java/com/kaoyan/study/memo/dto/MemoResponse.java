package com.kaoyan.study.memo.dto;

import com.kaoyan.study.memo.entity.Memo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 备忘录响应。 */
public record MemoResponse(
        Long id,
        Long subjectId,
        String content,
        LocalDate dueDate,
        String status,
        Long convertedTaskId,
        boolean reminderDismissed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {

    public static MemoResponse from(Memo memo) {
        return new MemoResponse(memo.getId(), memo.getSubjectId(), memo.getContent(), memo.getDueDate(),
                memo.getStatus(), memo.getConvertedTaskId(), memo.isReminderDismissed(),
                memo.getCreatedAt(), memo.getUpdatedAt(), memo.getVersion());
    }
}
