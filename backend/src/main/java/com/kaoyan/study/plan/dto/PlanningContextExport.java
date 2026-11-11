package com.kaoyan.study.plan.dto;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.memo.dto.MemoResponse;
import com.kaoyan.study.progress.dto.CatalogContracts;
import com.kaoyan.study.review.dto.AutoReviewContracts;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.dto.ReviewWorkspaceResponse.Trace;
import com.kaoyan.study.settings.dto.SubjectResponse;
import com.kaoyan.study.settings.dto.StudyUnitResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Portable read-only context, deliberately distinct from the plan import contract. */
public record PlanningContextExport(Snapshot snapshot, String markdown) {
    public record Snapshot(int schemaVersion, String documentType, OffsetDateTime exportedAt,
                           String dataVersion, LocalDate studyDate, LocalDate recentFrom,
                           LocalDate recentTo, List<String> rules, Facts facts, Feedback feedback) {}
    public record Facts(List<SubjectResponse> subjects, List<StudyUnitResponse> units,
                        List<StageGoalResponse> goals, List<Task> tasks, PlanWorkspaceResponse referencePlan,
                        CatalogContracts.Library progress, List<Continuation> continuations,
                        List<StudyRecordView> studyRecords, List<ReviewSource> reviewItems,
                        List<Trace> reviewRecords, List<AutoReviewContracts.Book> automaticReviewBooks,
                        List<AutoReviewContracts.Segment> automaticReviewSegments, List<MemoResponse> openMemos) {}
    public record Task(Long id, Long subjectId, Long stageGoalId, String title, Long unitId,
                       BigDecimal plannedAmount, String status, Long version, String activityMode,
                       String description, Sequence sequence, Actual actual) {}
    public record Actual(BigDecimal firstStudyAmount, BigDecimal reviewAmount,
                         BigDecimal contentPercent, int recordCount, long learningDays, long unknownQuantityRecords) {}
    public record Sequence(String mode, LocalDate startDate, LocalDate endDate, List<SequenceItem> items) {}
    public record SequenceItem(Long id, Integer position, String key, String title, String url) {}
    public record Continuation(Long taskId, Long streamId, String bookTitle, Integer position,
                               LocalDate referenceDate, String description, boolean referenceDaysOnly, LocalDate availableFrom) {}
    public record ReviewSource(ReviewItemResponse item, Long sourceRecordId) {}
    public record Feedback(int studyRecordCount, long learningDays, int reviewRecordCount,
                           long completedReviews, List<Quantity> quantities,
                           List<Long> studyRecordIds, List<Long> reviewRecordIds,
                           List<Weakness> weaknesses, List<String> limitations) {}
    public record Quantity(Long subjectId, Long unitId, BigDecimal firstStudyAmount, BigDecimal reviewAmount,
                           long unknownFirstStudyRecords, long unknownReviewRecords) {}
    public record Weakness(Long reviewItemId, String title, String result, LocalDate feedbackDate,
                           Long evidenceRecordId, String note) {}
}
