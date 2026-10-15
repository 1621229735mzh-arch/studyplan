package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.RecordArchive;
import com.kaoyan.study.learning.dto.RecordArchive.*;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.progress.dto.CatalogContracts.RecordClassification;
import com.kaoyan.study.progress.service.CatalogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecordArchiveService {
    private final StudyRecordService records;
    private final CatalogService catalog;
    public RecordArchiveService(StudyRecordService records,CatalogService catalog) {this.records=records;this.catalog=catalog;}
    @Transactional(readOnly=true)
    public RecordArchive archive(LocalDate from,LocalDate to,Long subjectId,Long taskId) {
        if(from!=null && to!=null && from.isAfter(to)) throw new BusinessException("INVALID_DATE_RANGE","开始日期不能晚于结束日期");
        var rows=records.list(from,to,taskId,subjectId); var classified=catalog.classifications(rows);
        var months=new ArrayList<Month>();
        for(var month:group(rows,r->YearMonth.from(r.recordDate()).toString()).entrySet()) {
            var ym=YearMonth.parse(month.getKey()); var weeks=new ArrayList<Week>();
            for(var week:group(month.getValue(),r->weekStart(r.recordDate()).toString()).entrySet()) {
                var start=LocalDate.parse(week.getKey()); var days=new ArrayList<Day>();
                for(var day:group(week.getValue(),r->r.recordDate().toString()).entrySet()) {
                    var groups=new ArrayList<Group>();
                    var grouped=day.getValue().stream().collect(Collectors.groupingBy(r->groupKey(r,classified.get(r.id())),LinkedHashMap::new,Collectors.toList()));
                    for(var entry:grouped.entrySet()) {
                        var list=entry.getValue(); var first=list.getFirst(); var c=classified.get(first.id());
                        groups.add(new Group(entry.getKey(),first.subjectId(),first.subjectName(),c==null ? null:c.courseId(),
                                c==null ? Objects.toString(first.taskTitle(),Objects.toString(first.content(),"自主学习")):c.courseTitle(),
                                first.unitId(),first.unitName(),amount(list),minutes(list),missing(list),list));
                    }
                    days.add(new Day(LocalDate.parse(day.getKey()),stats(day.getValue()),groups));
                }
                // 跨月周在各月内分段；同一条记录只进入其实际月份，周从周一开始。
                weeks.add(new Week(start,start.isBefore(ym.atDay(1))?ym.atDay(1):start,
                        start.plusDays(6).isAfter(ym.atEndOfMonth())?ym.atEndOfMonth():start.plusDays(6),stats(week.getValue()),days));
            }
            months.add(new Month(month.getKey(),stats(month.getValue()),weeks));
        }
        return new RecordArchive(stats(rows),months);
    }
    private LinkedHashMap<String,List<StudyRecordView>> group(List<StudyRecordView> rows,Function<StudyRecordView,String> key) {
        var grouped=rows.stream().collect(Collectors.groupingBy(key,TreeMap::new,Collectors.toList()));
        var result=new LinkedHashMap<String,List<StudyRecordView>>(); grouped.descendingMap().forEach(result::put); return result;
    }
    private String groupKey(StudyRecordView r,RecordClassification c) {
        return r.subjectId()+":"+r.unitId()+":"+(c!=null ? "course-"+c.courseId():r.taskId()!=null?"task-"+r.taskId():"content-"+Objects.toString(r.content(),"自主学习"));
    }
    private LocalDate weekStart(LocalDate day) {return day.minusDays(day.getDayOfWeek().getValue()-1L);}
    private BigDecimal amount(List<StudyRecordView> rows) {
        return rows.stream().allMatch(r->r.amount()==null)?null:rows.stream().map(StudyRecordView::amount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private Long minutes(List<StudyRecordView> rows) {
        return rows.stream().allMatch(r->r.durationMinutes()==null)?null:rows.stream().filter(r->r.durationMinutes()!=null).mapToLong(StudyRecordView::durationMinutes).sum();
    }
    private int missing(List<StudyRecordView> rows) {return (int)rows.stream().filter(r->r.durationMinutes()==null && !"CHECK_ONLY".equals(r.activityMode())).count();}
    private Stats stats(List<StudyRecordView> rows) {
        var quantities=new LinkedHashMap<String,Amount>();
        for(var r:rows) if(r.amount()!=null && r.subjectId()!=null && r.unitId()!=null) {
            String key=r.subjectId()+":"+r.unitId(); var old=quantities.get(key);
            quantities.put(key,new Amount(r.subjectId(),r.subjectName(),r.unitId(),r.unitName(),r.amount().add(old==null?BigDecimal.ZERO:old.amount())));
        }
        return new Stats((int)rows.stream().map(StudyRecordView::recordDate).distinct().count(),rows.size(),minutes(rows),missing(rows),new ArrayList<>(quantities.values()));
    }
}
