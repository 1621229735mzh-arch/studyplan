package com.kaoyan.study.learning.service;

import com.kaoyan.study.learning.mapper.ContinuationProgressMapper;
import com.kaoyan.study.plan.service.TaskContinuationService;
import com.kaoyan.study.settings.service.StudyDay;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

/** Read-only evidence from original, non-deleted records, never reference plan dates. */
@Service
public class CompletedStudySourceService {
    private final ContinuationProgressMapper records;
    private final TaskContinuationService continuations;
    private final StudyDay studyDay;
    public CompletedStudySourceService(ContinuationProgressMapper records, TaskContinuationService continuations, StudyDay studyDay) {
        this.records = records; this.continuations = continuations; this.studyDay = studyDay;
    }
    public LocalDate completedOn(Long taskId) {
        var streamId = continuations.streamId(taskId);
        if (streamId == null) return null;
        var rows = records.history(streamId).stream()
                .filter(r -> r.taskId().equals(taskId) && !r.deleted() && !r.recordDate().isAfter(studyDay.today()))
                .sorted(Comparator.comparing(ContinuationProgressMapper.Row::recordDate).thenComparing(ContinuationProgressMapper.Row::id)).toList();
        BigDecimal total = BigDecimal.ZERO;
        for (var row : rows) {
            if (row.delta() != null) total = total.add(row.delta());
            if (total.compareTo(BigDecimal.valueOf(100)) >= 0) return row.recordDate();
        }
        return null;
    }
}
