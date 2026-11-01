package com.kaoyan.study.review.dto;

import com.kaoyan.study.review.entity.MasteryResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 复习工作区只组合原复习项、原复习记录，不保存另一份完成事实。 */
public record ReviewWorkspaceResponse(List<Month> months, List<Entry> entries, Totals totals) {
    public record Totals(int itemCount, long reviewedItems, long completedReviews, long reviewDays, int recordCount) {}
    public record Month(String month, long scheduledDays, int entryCount, long completedCount) {}
    public record Entry(Long reviewItemId, Long subjectId, String title, LocalDate date,
                        BigDecimal completionPercent, MasteryResult result, boolean scheduled, Long version,
                        Long streamId, String bookTitle, LocalDate learnedOn, int round) {}
    public record Trace(Long id, Long reviewItemId, Long taskId, Long subjectId, String subjectName,
                        String title, LocalDate reviewDate, BigDecimal progressDelta, MasteryResult result,
                        Integer durationMinutes, String note) {}
    public record SubjectProgress(Long subjectId, String subjectName, Totals totals) {}
    public record SourceProgress(Long reviewItemId, Long taskId, Long sourceRecordId, Long subjectId,
                                 String subjectName, List<Trace> history) {}
}
