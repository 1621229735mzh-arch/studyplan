package com.kaoyan.study.progress.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.kaoyan.study.learning.dto.StudyRecordView;

/** 完整目录合同。日期明细属于学习记录，目录完成量不另存一份。 */
public final class CatalogContracts {
    private CatalogContracts() {}
    public record Document(Integer schemaVersion, List<CourseInput> courses) {}
    public record CourseInput(String key, String direction, String branch, String title,
                              Long subjectId, Long unitId, List<NodeInput> nodes, String progressMode) {
        public CourseInput(String key,String direction,String branch,String title,Long subjectId,Long unitId,List<NodeInput> nodes) {
            this(key,direction,branch,title,subjectId,unitId,nodes,null);
        }
    }
    public record NodeInput(String key, String parentKey, String title, BigDecimal totalAmount,
                            Integer sortOrder, List<Long> taskIds) {}
    public record SourceRequest(String document) {}
    public record ConfirmRequest(String previewToken) {}
    public record AssignRequest(Long nodeId, Long recordVersion, Long revision) {}
    public record TaskLinkRequest(List<Long> taskIds, Long revision) {}
    public record CourseChange(String title, String unitName, BigDecimal previousTotal,
                                BigDecimal nextTotal, int addedNodes, String progressMode, Integer previousItems, Integer nextItems) {}
    public record Preview(String previewToken, long revision, LocalDateTime expiresAt,
                          Document document, List<CourseChange> changes) {}
    public record Quantity(String direction, Long subjectId, String subjectName, Long unitId, String unitName, BigDecimal amount) {}
    public record RecordClassification(Long courseId, String courseTitle) {}
    public record ContentCounts(int total,int completed,int inProgress,int remaining) {}
    public record NodeView(Long id, String key, String parentKey, String title, BigDecimal totalAmount,
                           BigDecimal completedAmount, List<Long> taskIds, int sortOrder, BigDecimal completionPercent, ContentCounts contentCounts) {}
    public record CourseView(Long id, String key, String direction, String branch, String title,
                             Long subjectId, Long unitId, String unitName, BigDecimal totalAmount,
                             BigDecimal completedAmount, BigDecimal recordedAmount, List<NodeView> nodes,
                             List<Integer> completedProblemNumbers, String progressMode, ContentCounts contentCounts) {}
    public record CountedProgress(Long taskId, String title, String direction, String mode,
                                  Long subjectId, String subjectName, Long unitId, String unitName,
                                  BigDecimal recordedAmount, int recordCount, long learningDays,
                                  long unknownQuantityRecords) {}
    public record Library(long revision, List<CourseView> courses, List<Quantity> cumulative,
                           List<StudyRecordView> unclassified, List<CountedProgress> activities) {}
    public record TaskCandidate(Long id, String title, Long subjectId, Long unitId,
                                BigDecimal plannedAmount, Long version, Long nodeId) {}
}
