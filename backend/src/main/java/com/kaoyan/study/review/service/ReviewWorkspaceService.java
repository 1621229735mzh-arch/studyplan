package com.kaoyan.study.review.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.dto.ReviewWorkspaceResponse.*;
import com.kaoyan.study.review.entity.ReviewItem;
import com.kaoyan.study.review.entity.ReviewStatus;
import com.kaoyan.study.review.mapper.ReviewMapper;
import com.kaoyan.study.settings.service.StudySettingsService;
import com.kaoyan.study.settings.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewWorkspaceService {
    private final ReviewMapper mapper;
    private final SubjectService subjects;
    private final StudySettingsService settings;
    private final AutoReviewService automatic;
    public ReviewWorkspaceService(ReviewMapper mapper, SubjectService subjects, StudySettingsService settings, AutoReviewService automatic) {
        this.automatic = automatic;
        this.mapper = mapper; this.subjects = subjects; this.settings = settings;
    }

    @Transactional(readOnly = true)
    public ReviewWorkspaceResponse workspace(Long subjectId) {
        var items = mapper.findItems(null, subjectId);
        var history = history(null, null, subjectId, null);
        var entries = new ArrayList<Entry>();
        var segments=automatic.segments().stream().collect(Collectors.toMap(com.kaoyan.study.review.dto.AutoReviewContracts.Segment::reviewItemId,s->s));
        var books=automatic.books().stream().collect(Collectors.toMap(com.kaoyan.study.review.dto.AutoReviewContracts.Book::streamId,b->b.title()));
        for (var item : items) {
            var segment=segments.get(item.getId());
            var byDate = history.stream().filter(r -> r.reviewItemId().equals(item.getId()))
                    .collect(Collectors.groupingBy(Trace::reviewDate, TreeMap::new, Collectors.toList()));
            if (item.getStatus() == ReviewStatus.SCHEDULED && item.getScheduledDate() != null)
                byDate.computeIfAbsent(item.getScheduledDate(), d -> new ArrayList<>());
            byDate.forEach((date, rows) -> entries.add(new Entry(item.getId(), item.getSubjectId(), item.getTitle(), date,
                    rows.stream().map(Trace::progressDelta).reduce(BigDecimal.ZERO, BigDecimal::add).min(BigDecimal.valueOf(100)),
                    rows.stream().map(Trace::result).filter(Objects::nonNull).findFirst().orElse(null),
                    item.getStatus() == ReviewStatus.SCHEDULED && date.equals(item.getScheduledDate()), item.getVersion(),
                    segment==null ? null : segment.streamId(), segment==null ? null : books.get(segment.streamId()),
                    segment==null ? null : segment.completedOn(),
                    1+(int)history.stream().filter(r->r.reviewItemId().equals(item.getId()) && r.result()!=null && r.reviewDate().isBefore(date)).count())));
        }
        entries.sort(Comparator.comparing(Entry::date).thenComparing(Entry::reviewItemId));
        var months = entries.stream().collect(Collectors.groupingBy(e -> e.date().toString().substring(0, 7), TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(e -> new Month(e.getKey(), e.getValue().stream().map(Entry::date).distinct().count(),
                        e.getValue().size(), e.getValue().stream().filter(x -> x.result() != null).count())).toList();
        return new ReviewWorkspaceResponse(months, entries, totals(items, history));
    }

    public List<Trace> history(LocalDate from, LocalDate to, Long subjectId, Long taskId) {
        if (from != null && to != null && from.isAfter(to))
            throw new BusinessException("INVALID_DATE_RANGE", "开始日期不能晚于结束日期");
        return mapper.findHistory(from, to, subjectId, taskId);
    }

    @Transactional(readOnly = true)
    public List<SubjectProgress> progress() {
        var items = mapper.findItems(null, null);
        var rows = history(null, null, null, null);
        return subjects.list(false).stream().map(s -> new SubjectProgress(s.getId(), s.getName(),
                totals(items.stream().filter(i -> s.getId().equals(i.getSubjectId())).toList(),
                        rows.stream().filter(r -> s.getId().equals(r.subjectId())).toList())))
                .filter(s -> s.totals().itemCount() > 0).toList();
    }

    /** Internal read-only facts for exact course/branch attribution by the progress module. */
    public List<SourceProgress> progressSources() {
        var names=subjects.list(false).stream().collect(Collectors.toMap(s->s.getId(),s->s.getName()));
        var traces=history(null,null,null,null).stream().collect(Collectors.groupingBy(Trace::reviewItemId));
        return mapper.findItems(null,null).stream().map(i->new SourceProgress(i.getId(),i.getTaskId(),i.getSourceRecordId(),
                i.getSubjectId(),names.get(i.getSubjectId()),traces.getOrDefault(i.getId(),List.of()))).toList();
    }
    public Totals totalsForSources(List<SourceProgress> sources) {
        var rows=sources.stream().flatMap(s->s.history().stream()).toList();
        var completed=rows.stream().filter(r->r.result()!=null).toList();
        return new Totals(sources.size(),completed.stream().map(Trace::reviewItemId).distinct().count(),
                completed.stream().map(r->r.reviewItemId()+":"+r.reviewDate()).distinct().count(),
                rows.stream().map(Trace::reviewDate).distinct().count(),rows.size());
    }

    private Totals totals(List<ReviewItem> items, List<Trace> rows) {
        var completed = rows.stream().filter(r -> r.result() != null).toList();
        return new Totals(items.size(), completed.stream().map(Trace::reviewItemId).distinct().count(),
                completed.stream().map(r -> r.reviewItemId() + ":" + r.reviewDate()).distinct().count(),
                rows.stream().map(Trace::reviewDate).distinct().count(), rows.size());
    }

    /** 延续现有每项一个待执行日期的规则；调整安排不改写已有记录。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReviewItemResponse schedule(ReviewScheduleRequest request) {
        automatic.lockSource(request.reviewItemId());
        // 与旧确认入口保持相同锁顺序，避免并发确认和调整相互覆盖。
        settings.lockForReviewScheduling();
        var item = mapper.findItemByIdForUpdate(request.reviewItemId());
        if (item == null) throw new NotFoundException("复习项不存在");
        if (!item.getVersion().equals(request.version()))
            throw new ConflictException("REVIEW_ITEM_STALE", "复习内容已变化，请刷新后重新核对");
        if (item.getStatus() == ReviewStatus.ARCHIVED)
            throw new BusinessException("REVIEW_ARCHIVED", "已归档内容不能安排复习");
        automatic.manualSchedule(item,request.date());
        if (request.date() != null && mapper.findDayTasks(request.date()).stream().anyMatch(r ->
                r.id().equals(item.getId()) && r.completionPercent().compareTo(BigDecimal.valueOf(100)) >= 0))
            throw new BusinessException("REVIEW_ALREADY_COMPLETED", "当天已完成该内容，请选择其他日期");
        if (mapper.updateItemSchedule(item.getId(), request.date() == null ? ReviewStatus.PENDING : ReviewStatus.SCHEDULED,
                request.date(), item.getEstimatedMinutes(), item.getVersion()) == 0)
            throw new ConflictException("REVIEW_ITEM_STALE", "复习内容已变化，请刷新后重新核对");
        return ReviewItemResponse.from(mapper.findItemById(item.getId()));
    }
}
