package com.kaoyan.study.review.service;

import com.kaoyan.study.common.exception.*;
import com.kaoyan.study.learning.dto.StudyTaskChanged;
import com.kaoyan.study.learning.service.CompletedStudySourceService;
import com.kaoyan.study.plan.entity.TaskStatus;
import com.kaoyan.study.plan.service.TaskContinuationService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.review.dto.AutoReviewContracts.*;
import com.kaoyan.study.review.dto.AutomaticReviewContext;
import com.kaoyan.study.review.dto.DayReviewTaskView;
import com.kaoyan.study.review.dto.QueuedReviewTask;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.mapper.*;
import com.kaoyan.study.settings.service.StudyDay;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class AutoReviewService {
    private final AutoReviewMapper auto;
    private final ReviewMapper reviews;
    private final TaskService tasks;
    private final TaskContinuationService continuations;
    private final CompletedStudySourceService sources;
    private final StudyDay studyDay;
    public AutoReviewService(AutoReviewMapper auto, ReviewMapper reviews, TaskService tasks,
            TaskContinuationService continuations, CompletedStudySourceService sources, StudyDay studyDay) {
        this.auto=auto; this.reviews=reviews; this.tasks=tasks; this.continuations=continuations; this.sources=sources; this.studyDay=studyDay;
    }
    public List<Book> books() { return auto.books(studyDay.today()); }
    public List<Segment> segments() { return auto.segments(); }

    /** One current recommendation per book, then opt-in continuation after a real completion today. */
    public List<QueuedReviewTask> dayQueue(LocalDate date) {
        if (!date.equals(studyDay.today())) return List.of();
        var groups=auto.queue(date).stream().collect(Collectors.groupingBy(AutoReviewMapper.QueueRow::streamId,
                LinkedHashMap::new,Collectors.toList()));
        var result=new ArrayList<QueuedReviewTask>();
        for (var rows:groups.values()) {
            var completed=rows.stream().filter(AutoReviewMapper.QueueRow::completed).toList();
            var pending=rows.stream().filter(r->r.eligible() && !r.completed())
                    .sorted(Comparator.<AutoReviewMapper.QueueRow,Boolean>comparing(r->r.completionPercent().signum()==0)
                        .thenComparing(r->!r.dueDate().isBefore(date))
                        .thenComparing(Comparator.comparingDouble((AutoReviewMapper.QueueRow r)->urgency(r,date)).reversed())
                        .thenComparingInt(r->"FORGOT".equals(r.lastResult()) ? 0 : "VAGUE".equals(r.lastResult()) ? 1 : 2)
                        .thenComparing(AutoReviewMapper.QueueRow::dueDate).thenComparing(AutoReviewMapper.QueueRow::id)).toList();
            for (var row:completed) result.add(queueTask(row,false,pending.size(),date,false));
            if (!pending.isEmpty()) {
                var row=pending.getFirst();
                result.add(queueTask(row,!completed.isEmpty() && row.completionPercent().signum()==0,pending.size(),date,true));
            }
        }
        return result;
    }
    private static double urgency(AutoReviewMapper.QueueRow row,LocalDate date) {
        return (double)ChronoUnit.DAYS.between(row.dueDate(),date)/Math.max(1,row.intervalDays()==null ? 1 : row.intervalDays());
    }
    private static QueuedReviewTask queueTask(AutoReviewMapper.QueueRow row,boolean optional,int count,LocalDate date,boolean actionable) {
        return new QueuedReviewTask(new DayReviewTaskView(row.id(),row.subjectId(),row.title(),row.completionPercent(),row.estimatedMinutes(),
                row.actualMinutes(),row.missingDurations(),row.version(),actionable),
                new AutomaticReviewContext(row.streamId(),row.bookTitle(),row.completed() ? null : row.dueDate(),
                    row.completed() ? 0 : Math.max(0,ChronoUnit.DAYS.between(row.dueDate(),date)),
                    row.completed() ? row.repetitions() : row.repetitions()+1,optional,count));
    }

    /** Shared order with learning writes: all task locks (sorted), stream, then review items. */
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Book setPolicy(Long streamId, PolicyRequest request) {
        var ids=auto.taskIds(streamId);
        if (ids.isEmpty()) throw new NotFoundException("教材内容不存在");
        ids.forEach(tasks::requireExistingForUpdate);
        continuations.lockForTasks(ids.getFirst());
        auto.ensurePolicy(streamId);
        var current=books().stream().filter(b->b.streamId().equals(streamId)).findFirst().orElseThrow();
        if (!current.version().equals(request.version())) throw new ConflictException("REVIEW_POLICY_STALE","自动复习规则已变化，请刷新后重试");
        if (current.enabled()==request.enabled()) return current;
        if (auto.policy(streamId,request.enabled(),request.version())!=1) throw new ConflictException("REVIEW_POLICY_STALE","自动复习规则已变化");
        ids.forEach(this::syncTask);
        return books().stream().filter(b->b.streamId().equals(streamId)).findFirst().orElseThrow();
    }

    @EventListener
    @Transactional(propagation=Propagation.MANDATORY)
    public void learned(StudyTaskChanged event) { if (event.taskId()!=null) syncTask(event.taskId()); }

    private void syncTask(Long taskId) {
        Long stream=continuations.streamId(taskId);
        if (stream==null) return;
        boolean enabled=Boolean.TRUE.equals(auto.enabled(stream));
        var existing=reviews.findItemByTaskId(taskId);
        if (!enabled && (existing==null || auto.state(existing.getId())==null)) return;
        LocalDate completed=tasks.get(taskId).getStatus()==TaskStatus.ACTIVE ? sources.completedOn(taskId) : null;
        if (existing==null) {
            if (completed==null) return;
            var task=tasks.get(taskId);
            var item=new ReviewItem(); item.setTaskId(taskId); item.setSubjectId(task.getSubjectId()); item.setTitle(task.getTitle());
            item.setStatus(ReviewStatus.SCHEDULED); item.setNextReviewDate(completed.plusDays(1)); item.setScheduledDate(completed.plusDays(1));
            reviews.insertItem(item); auto.insert(item.getId(),stream,completed); return;
        }
        // A pre-existing manually managed item is never silently converted or rescheduled.
        var item=reviews.findItemByIdForUpdate(existing.getId());
        var state=auto.state(item.getId());
        if (state==null) return;
        boolean changed=state.sourceValid()!=(completed!=null) || completed!=null && !completed.equals(state.completedOn());
        auto.source(item.getId(),completed,completed!=null);
        if (item.getStatus()==ReviewStatus.ARCHIVED) return;
        if (!enabled || completed==null || state.paused()) {
            if (item.getStatus()==ReviewStatus.SCHEDULED) schedule(item,null);
            else if (changed) reviews.touchItem(item.getId(),item.getVersion());
            return;
        }
        LocalDate due=changed && state.lastReviewDate()==null ? completed.plusDays(1) : item.getNextReviewDate();
        // Do not revive old invalid chronology after a source was deleted and completed again later.
        if (due==null || due.isBefore(completed.plusDays(1))) due=completed.plusDays(1);
        if (item.getStatus()!=ReviewStatus.SCHEDULED || changed) {
            if (reviews.updateAutomaticDue(item.getId(),due,item.getVersion())!=1)
                throw new ConflictException("REVIEW_ITEM_STALE","复习来源或安排已变化");
        }
    }

    /** Called before locking a review item, never while already holding its lock. */
    public void lockSource(Long itemId) {
        lockSources(List.of(itemId));
    }
    public void lockSources(List<Long> itemIds) {
        var taskIds=itemIds.stream().filter(id->auto.state(id)!=null).map(reviews::findItemById)
                .map(ReviewItem::getTaskId).distinct().sorted().toList();
        taskIds.forEach(tasks::requireExistingForUpdate);
        continuations.lockForTasks(taskIds.toArray(Long[]::new));
    }
    public boolean managed(Long itemId) { return auto.state(itemId)!=null; }
    public void requireSource(ReviewItem item) {
        var state=auto.state(item.getId());
        if (state!=null && (!state.sourceValid() || tasks.get(item.getTaskId()).getStatus()!=TaskStatus.ACTIVE || sources.completedOn(item.getTaskId())==null))
            throw new BusinessException("REVIEW_SOURCE_INCOMPLETE","来源内容已不满足完整学习条件，请先完成学习；已有复习历史保留");
    }
    public void manualSchedule(ReviewItem item,LocalDate date) {
        var state=auto.state(item.getId());
        if (state==null) return;
        requireSource(item);
        if (date!=null && !Boolean.TRUE.equals(auto.enabled(state.streamId())))
            throw new BusinessException("REVIEW_AUTO_PAUSED","请先启用该教材的自动复习");
        if (date!=null && (date.isBefore(state.completedOn().plusDays(1))
                || state.lastReviewDate()!=null && !date.isAfter(state.lastReviewDate())))
            throw new BusinessException("REVIEW_DATE_INVALID","复习安排必须晚于实际完成日及最近完整复习日");
        auto.pause(item.getId(),date==null);
        if (date!=null && reviews.updateAutomaticReminder(item.getId(),date,item.getVersion())!=1)
            throw new ConflictException("REVIEW_ITEM_STALE","复习安排已变化");
    }
    public void requireProgress(ReviewItem item,LocalDate date) {
        var state=auto.state(item.getId());
        if (state==null) return;
        requireSource(item);
        if (state.paused() || !Boolean.TRUE.equals(auto.enabled(state.streamId())))
            throw new BusinessException("REVIEW_AUTO_PAUSED","自动复习已暂停，请重新启用或安排后再反馈");
        if (date.isAfter(studyDay.today()) || !date.isAfter(state.completedOn())
                || state.lastReviewDate()!=null && !date.isAfter(state.lastReviewDate()))
            throw new BusinessException("REVIEW_DATE_INVALID","复习日期必须晚于实际学完日及上次完整复习日，且不能是未来日期");
    }
    public record Next(LocalDate date,int intervalDays) {}
    public Next feedback(ReviewItem item,LocalDate date,MasteryResult result,LocalDate override) {
        var state=auto.state(item.getId());
        if (state==null) return null;
        requireProgress(item,date);
        if (!Objects.equals(state.algorithm(),FsrsScheduler.VERSION)) throw new IllegalStateException("Unknown FSRS version");
        var memory=state.stability()==null ? null : new FsrsScheduler.Memory(state.stability(),state.difficulty());
        var next=FsrsScheduler.review(memory,state.lastReviewDate()==null ? 0 : Math.toIntExact(ChronoUnit.DAYS.between(state.lastReviewDate(),date)),result);
        if (override!=null && !override.isAfter(date)) throw new BusinessException("REVIEW_DATE_INVALID","下次复习必须安排在本次复习之后");
        LocalDate due=override==null ? date.plusDays(next.intervalDays()) : override;
        auto.memory(item.getId(),next.memory().stability(),next.memory().difficulty(),date);
        return new Next(due,Math.toIntExact(ChronoUnit.DAYS.between(date,due)));
    }
    private void schedule(ReviewItem item,LocalDate date) {
        if (reviews.updateItemSchedule(item.getId(),date==null ? ReviewStatus.PENDING : ReviewStatus.SCHEDULED,date,item.getEstimatedMinutes(),item.getVersion())!=1)
            throw new ConflictException("REVIEW_ITEM_STALE","复习安排已变化");
    }

    /** For future-dated learning becoming eligible at the next 09:00 cutoff; also recovers downtime. */
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void reconcile(Long streamId) {
        var ids=auto.taskIds(streamId); ids.forEach(tasks::requireExistingForUpdate);
        if (!ids.isEmpty()) continuations.lockForTasks(ids.getFirst());
        ids.forEach(this::syncTask);
    }
}
