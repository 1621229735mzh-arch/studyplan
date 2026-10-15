package com.kaoyan.study.learning.service;

import com.kaoyan.study.learning.dto.DayLearningTaskView;
import com.kaoyan.study.learning.mapper.ContinuationProgressMapper;
import com.kaoyan.study.learning.mapper.ContinuationProgressMapper.Row;
import com.kaoyan.study.plan.service.TaskActivityService;
import com.kaoyan.study.plan.service.TaskContinuationService;
import com.kaoyan.study.plan.service.TaskContinuationService.Definition;
import com.kaoyan.study.plan.service.PlanImportService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Derives admission dates and percentages from records; reading never moves or creates plans. */
@Service
public class ContinuationLearningService {
    public record Context(Long streamId,String title,Integer position,LocalDate referenceDate,boolean carried) {}
    private final TaskContinuationService plans;
    private final ContinuationProgressMapper records;
    public ContinuationLearningService(TaskContinuationService plans,ContinuationProgressMapper records) {
        this.plans=plans; this.records=records;
    }
    public BigDecimal total(Long taskId) { return records.total(taskId); }
    public List<DayLearningTaskView> dayTasks(LocalDate date) {
        var groups=plans.all().stream().collect(Collectors.groupingBy(Definition::streamId,LinkedHashMap::new,Collectors.toList()));
        var result=new ArrayList<DayLearningTaskView>();
        for (var group:groups.entrySet()) result.addAll(project(group.getValue(),records.history(group.getKey()),date));
        return result;
    }

    /** A positive delta on the newest admitted chunk unlocks one new chunk on the next date.
     * Finishing an older remainder does not unlock anything. Calendar gaps never accumulate slots. */
    public static List<DayLearningTaskView> project(List<Definition> definitions,List<Row> history,LocalDate date) {
        var ordered=definitions.stream().sorted(Comparator.comparing(Definition::position)).toList();
        var active=history.stream().filter(r->!r.deleted() && !r.recordDate().isAfter(date)).toList();
        String revision=PlanImportService.digest(ordered.toString()+history.toString()+date);
        var result=new ArrayList<DayLearningTaskView>();
        LocalDate admitted=null;
        List<Row> previousRows=List.of();
        for (var definition:ordered) {
            var own=active.stream().filter(r->r.taskId().equals(definition.taskId())).toList();
            if (admitted==null && definition.equals(ordered.getFirst())) admitted=definition.referenceDate();
            else if (admitted!=null) {
                final LocalDate previousAdmission=admitted;
                var firstProgress=previousRows.stream().filter(r->r.delta()!=null && r.delta().signum()>0
                        && !r.recordDate().isBefore(previousAdmission)).map(Row::recordDate).min(LocalDate::compareTo).orElse(null);
                admitted=firstProgress==null ? null : firstProgress.plusDays(1);
                if (admitted!=null && admitted.isBefore(definition.referenceDate())) admitted=definition.referenceDate();
            }
            // A correction may reopen an older chunk. Retain later records with their true task identity.
            var ownDate=own.stream().map(Row::recordDate).min(LocalDate::compareTo).orElse(null);
            if (ownDate!=null && (admitted==null || ownDate.isBefore(admitted))) admitted=ownDate;
            previousRows=own;
            if (admitted==null || date.isBefore(admitted)) continue;
            var before=percent(own.stream().filter(r->r.recordDate().isBefore(date)).toList());
            var completed=percent(own);
            if (before.compareTo(BigDecimal.valueOf(100))>=0 && own.stream().noneMatch(r->r.recordDate().equals(date))) continue;
            var remaining=BigDecimal.valueOf(definition.estimatedMinutes()).multiply(BigDecimal.ONE.subtract(completed.movePointLeft(2)))
                    .setScale(1,RoundingMode.HALF_UP);
            var activity=new TaskActivityService.Activity("CONTINUED_STUDY",definition.description(),0);
            result.add(new DayLearningTaskView(definition.taskId(),definition.subjectId(),definition.title(),null,null,completed,
                    definition.estimatedMinutes(),remaining,revision,null,activity,definition.unitName(),
                    new Context(definition.streamId(),definition.streamTitle(),definition.position(),definition.referenceDate(),admitted.isBefore(date))));
        }
        return result;
    }
    private static BigDecimal percent(List<Row> rows) {
        return rows.stream().map(Row::delta).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add).min(BigDecimal.valueOf(100));
    }
}
