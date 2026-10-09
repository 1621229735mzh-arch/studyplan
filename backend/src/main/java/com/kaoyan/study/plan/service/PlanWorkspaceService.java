package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.dto.PlanWorkspaceResponse.*;
import com.kaoyan.study.plan.mapper.WorkspaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class PlanWorkspaceService {
    private final WorkspaceMapper mapper;
    private final DailyPlanService daily;
    private final TaskService tasks;
    public PlanWorkspaceService(WorkspaceMapper mapper, DailyPlanService daily, TaskService tasks) {
        this.mapper=mapper; this.daily=daily; this.tasks=tasks;
    }
    @Transactional(readOnly=true)
    public PlanWorkspaceResponse read(LocalDate start, LocalDate end, Long subjectId) {
        if (start!=null && end!=null && start.isAfter(end)) throw new BusinessException("INVALID_RANGE","开始日期不能晚于结束日期");
        var entries=mapper.entries(start,end,subjectId);
        var groups=new TreeMap<String,List<ScheduleEntryView>>();
        entries.forEach(e -> groups.computeIfAbsent(YearMonth.from(e.date()).toString(), k -> new ArrayList<>()).add(e));
        var outlines=mapper.outlines();
        var imports=mapper.imports();
        if (start==null && end==null && subjectId==null) {
            for (var p:imports) for (var m=YearMonth.from(p.startDate()); !m.isAfter(YearMonth.from(p.endDate())); m=m.plusMonths(1))
                groups.computeIfAbsent(m.toString(),k -> new ArrayList<>());
        }
        var months=new ArrayList<Month>();
        groups.forEach((month, rows) -> {
            var amounts=new LinkedHashMap<String,Amount>();
            for (var e:rows) {
                var key=e.subjectId()+":"+e.unitId();
                var old=amounts.getOrDefault(key,new Amount(e.subjectId(),e.unitId(),e.unitName(),BigDecimal.ZERO,BigDecimal.ZERO));
                amounts.put(key,new Amount(e.subjectId(),e.unitId(),e.unitName(),old.plannedAmount().add(e.plannedAmount()),old.completedAmount().add(e.completedAmount())));
            }
            months.add(new Month(month,(int) rows.stream().map(ScheduleEntryView::date).distinct().count(),
                    (int) rows.stream().map(ScheduleEntryView::taskId).distinct().count(),List.copyOf(amounts.values())));
        });
        return new PlanWorkspaceResponse(mapper.revision(),months,entries,outlines,imports);
    }
    /** 快速新增在一个事务中创建任务及当天安排；重试使用唯一令牌。 */
    @Transactional
    public List<DailyPlanItemView> quickAdd(LocalDate date, QuickDayRequest r) {
        mapper.lock();
        String hash=PlanImportService.digest(date+":"+r);
        String saved=mapper.quickHash(r.clientToken());
        if (saved!=null) {
            if (!saved.equals(hash)) throw new ConflictException("SUBMISSION_CHANGED","本次提交内容已变化，请重新提交");
            return daily.list(date);
        }
        List<DailyPlanItemView> result;
        if (r.existingTaskId()!=null) {
            if (daily.list(date).stream().anyMatch(i -> i.taskId().equals(r.existingTaskId())))
                throw new ConflictException("TASK_ALREADY_SCHEDULED","该任务当天已有安排，请使用编辑入口");
            result=daily.addItem(date,new DailyItemCreateRequest(r.existingTaskId(),r.plannedAmount(),null,r.estimatedMinutes()));
        } else {
            if (r.subjectId()==null || r.unitId()==null || r.title()==null || r.title().isBlank())
                throw new BusinessException("INVALID_TASK","请填写学习内容、科目和单位");
            var task=tasks.create(new TaskCreateRequest(r.subjectId(),null,r.title(),r.unitId(),r.plannedAmount()));
            result=daily.addItem(date,new DailyItemCreateRequest(task.getId(),r.plannedAmount(),null,r.estimatedMinutes()));
        }
        mapper.rememberQuick(r.clientToken(),hash,date);
        return result;
    }
    @Transactional
    public List<DailyPlanItemView> reorder(LocalDate date, ReorderRequest request) {
        mapper.lock();
        var existing=daily.list(date);
        var ids=new HashSet<Long>();
        for (var item:request.items()) if (!ids.add(item.id())) throw new BusinessException("INVALID_ORDER","排序包含重复条目");
        if (!ids.equals(new HashSet<>(existing.stream().map(DailyPlanItemView::id).toList())))
            throw new ConflictException("DAILY_ITEM_STALE","当天安排已变化，请刷新后重新排序");
        int order=0;
        for (var item:request.items()) if (mapper.reorder(date,item.id(),item.version(),order++)!=1)
            throw new ConflictException("DAILY_ITEM_STALE","当天安排已变化，请刷新后重新排序");
        mapper.touch();
        return daily.list(date);
    }
}
