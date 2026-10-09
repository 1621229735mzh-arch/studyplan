package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.entity.PlanSource;
import com.kaoyan.study.plan.mapper.WorkspaceMapper;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.settings.service.StudyUnitService;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlanImportService {
    private final JsonMapper json=JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final Validator validator;
    private final WorkspaceMapper mapper;
    private final SubjectService subjects;
    private final StudyUnitService units;
    private final TaskService tasks;
    private final StageGoalService goals;
    private final DailyPlanService daily;

    public PlanImportService(Validator validator,WorkspaceMapper mapper,SubjectService subjects,
            StudyUnitService units,TaskService tasks,StageGoalService goals,DailyPlanService daily) {
        this.validator=validator; this.mapper=mapper; this.subjects=subjects; this.units=units;
        this.tasks=tasks; this.goals=goals; this.daily=daily;
    }

    @Transactional(readOnly=true)
    public PlanImportPreview preview(String source) {
        var doc=parse(source);
        validate(doc);
        var hash=contentHash(doc);
        if (mapper.imported(doc.planId(),hash)>0)
            throw new ConflictException("PLAN_ALREADY_IMPORTED","这份方案已导入，不能重复追加；调整安排请使用日期中的编辑入口");
        var entries=mapper.entries(doc.startDate(),doc.endDate(),null);
        var warnings=new ArrayList<String>();
        var taskMap=unique(doc.tasks(),PlanImportDocument.Task::key,"tasks.key");
        var subjectMap=subjects.list(false).stream().collect(Collectors.toMap(s -> s.getId(),s -> s.getName()));
        var existingNames=entries.stream().map(e -> e.date()+":"+subjectMap.get(e.subjectId())+":"+
                e.taskTitle().trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        for (var day:doc.days()) for (var item:day.items()) {
            var task=taskMap.get(item.taskKey());
            boolean duplicate=existingNames.contains(day.date()+":"+task.subject().trim()+":"+task.title().trim().toLowerCase(Locale.ROOT));
            if (duplicate && warnings.size()<100)
                warnings.add(day.date()+" · "+task.title()+"：当天已有同科同名任务，导入仍会追加，请核对是否重复");
        }
        long revision=mapper.revision();
        // 绑定文件及预览时的数据快照。目录、任务或日安排变化后必须重新预览。
        String token=digest(doc.planId()+":"+hash+":"+revision+":"+json.writeValueAsString(entries)
                +json.writeValueAsString(subjects.list(false))+json.writeValueAsString(units.list(false)));
        return new PlanImportPreview(token,revision,doc,doc.tasks().size(),doc.days().size(),
                doc.days().stream().mapToInt(d -> d.items().size()).sum(),warnings);
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public PlanImportPreview confirm(PlanImportRequest request) {
        mapper.lock();
        var preview=preview(request.document());
        if (request.previewToken()==null || !MessageDigest.isEqual(preview.previewToken().getBytes(StandardCharsets.UTF_8),
                request.previewToken().getBytes(StandardCharsets.UTF_8)))
            throw new ConflictException("PLAN_PREVIEW_STALE","文件或计划数据已变化，请重新预览后确认导入");
        var doc=preview.document();
        mapper.insertImport(doc.planId(),contentHash(doc),doc.title(),doc.startDate(),doc.endDate());
        Long importId=mapper.importId(doc.planId());
        var goalIds=new HashMap<String,Long>();
        for (var g:doc.goals()) {
            Long subjectId=subjectId(g.subject());
            var goal=goals.create(new StageGoalCreateRequest(subjectId,g.title(),g.description(),g.startDate(),g.endDate()));
            goalIds.put(g.key(),goal.getId());
        }
        for (var m:doc.months()) mapper.insertOutline(importId,"MONTH",YearMonth.parse(m.month()).atDay(1),m.title(),m.description());
        for (var w:doc.weeks()) mapper.insertOutline(importId,"WEEK",w.startDate(),w.title(),w.description());
        var taskIds=new HashMap<String,Long>();
        for (var t:doc.tasks()) {
            var task=tasks.create(new TaskCreateRequest(subjectId(t.subject()),
                    t.goalKey()==null ? null:goalIds.get(t.goalKey()),t.title(),unitId(t.unit()),t.plannedAmount()));
            taskIds.put(t.key(),task.getId());
        }
        for (var day:doc.days()) for (var item:day.items())
            daily.appendImported(day.date(),new DailyItemCreateRequest(taskIds.get(item.taskKey()),item.amount(),PlanSource.IMPORT,item.estimatedMinutes()));
        mapper.touch();
        return preview;
    }

    public PlanImportDocument parse(String source) {
        if (source==null || source.getBytes(StandardCharsets.UTF_8).length>2000000) invalid("文件", "文件为空或超过 2 MB");
        try {
            var tree=json.readTree(source);
            checkTypes(tree);
            var doc=json.treeToValue(tree,PlanImportDocument.class);
            var errors=validator.validate(doc);
            if (!errors.isEmpty()) invalid("字段",errors.stream()
                    .sorted(Comparator.comparing(e -> e.getPropertyPath().toString()))
                    .limit(8).map(e -> e.getPropertyPath()+"："+e.getMessage()).collect(Collectors.joining("；")));
            // 容器内的 null 元素不能越过 @Valid 校验。
            if (doc.goals().contains(null) || doc.months().contains(null) || doc.weeks().contains(null)
                    || doc.tasks().contains(null) || doc.days().contains(null)) invalid("列表","不能包含 null 元素");
            if (doc.days().stream().anyMatch(d -> d.items().contains(null))) invalid("days.items","不能包含 null 元素");
            return doc;
        } catch (BusinessException ex) { throw ex; }
        catch (Exception ex) {
            throw new BusinessException("INVALID_PLAN_JSON","JSON 格式不符合规划书："+safeMessage(ex),ex);
        }
    }

    private void checkTypes(tools.jackson.databind.JsonNode root) {
        if (root==null || !root.isObject()) invalid("根节点","必须是 JSON 对象");
        // 禁止 Jackson 把数字/布尔值隐式转换为文本，或把字符串隐式转换为数量。
        checkNode(root,"");
    }
    private void checkNode(tools.jackson.databind.JsonNode node,String path) {
        if (node.isObject()) {
            node.properties().forEach(e -> {
                String name=e.getKey(); var value=e.getValue(); String p=path.isEmpty()?name:path+"."+name;
                if (Set.of("schemaVersion","plannedAmount","amount","estimatedMinutes").contains(name)) {
                    if (!value.isNull() && !value.isNumber()) invalid(p,"必须是 JSON 数字");
                    if (Set.of("schemaVersion","estimatedMinutes").contains(name) && !value.isNull()
                            && (!value.isIntegralNumber() || !value.canConvertToInt())) invalid(p,"必须是整数");
                } else if (!Set.of("goals","months","weeks","tasks","days","items").contains(name)
                        && !value.isNull() && !value.isString()) invalid(p,"必须是字符串");
                if (Set.of("startDate","endDate","date").contains(name) && value.isString()) {
                    try {
                        if (!value.asString().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) invalid(p,"必须使用 YYYY-MM-DD");
                        LocalDate.parse(value.asString());
                    } catch (java.time.DateTimeException ex) { invalid(p,"日期不存在"); }
                }
                checkNode(value,p);
            });
        } else if (node.isArray()) {
            for (int i=0;i<node.size();i++) checkNode(node.get(i),path+"["+i+"]");
        }
    }

    public void validate(PlanImportDocument doc) {
        if (doc.schemaVersion()!=1) invalid("schemaVersion","仅支持版本 1");
        range(doc.startDate(),doc.endDate(),doc.startDate(),doc.endDate(),"起止日期");
        if (doc.startDate().getYear()<1000 || doc.endDate().getYear()>9999) invalid("日期","年份必须在 1000—9999 范围内");
        long days=ChronoUnit.DAYS.between(doc.startDate(),doc.endDate())+1;
        if (days>5000 || doc.days().size()!=days) invalid("days","必须逐日覆盖全部起止日期；休息日填写空 items");
        var goalMap=unique(doc.goals(),PlanImportDocument.Goal::key,"goals.key");
        var taskMap=unique(doc.tasks(),PlanImportDocument.Task::key,"tasks.key");
        var subjectNames=subjects.list(true).stream().map(s -> s.getName()).collect(Collectors.toSet());
        var unitNames=units.list(true).stream().map(u -> u.getName()).collect(Collectors.toSet());
        for (var g:doc.goals()) {
            if (!subjectNames.contains(g.subject().trim())) invalid("goals."+g.key()+".subject","科目未配置或未启用："+g.subject());
            range(g.startDate(),g.endDate(),doc.startDate(),doc.endDate(),"goals."+g.key());
        }
        for (var t:doc.tasks()) {
            if (!subjectNames.contains(t.subject().trim())) invalid("tasks."+t.key()+".subject","科目未配置或未启用："+t.subject());
            if (!unitNames.contains(t.unit().trim())) invalid("tasks."+t.key()+".unit","单位未配置或未启用："+t.unit());
            if (t.goalKey()!=null) {
                var g=goalMap.get(t.goalKey());
                if (g==null || !g.subject().trim().equals(t.subject().trim())) invalid("tasks."+t.key()+".goalKey","目标不存在或科目不一致");
            }
        }
        var dates=new HashSet<LocalDate>(); var sums=new HashMap<String,BigDecimal>(); int count=0;
        for (var d:doc.days()) {
            range(d.date(),d.date(),doc.startDate(),doc.endDate(),"days."+d.date());
            if (!dates.add(d.date())) invalid("days."+d.date(),"日期重复");
            var dayKeys=new HashSet<String>();
            for (var item:d.items()) {
                count++;
                var t=taskMap.get(item.taskKey());
                if (t==null) invalid("days."+d.date(),"未知 taskKey："+item.taskKey());
                if (!dayKeys.add(item.taskKey())) invalid("days."+d.date(),"同一任务同一天只能出现一次");
                if (t.goalKey()!=null) {
                    var g=goalMap.get(t.goalKey());
                    range(d.date(),d.date(),g.startDate(),g.endDate(),"days."+d.date()+"."+item.taskKey());
                }
                sums.merge(item.taskKey(),item.amount(),BigDecimal::add);
            }
        }
        if (count>20000) invalid("days.items","最多导入 20000 条安排");
        for (var t:doc.tasks()) if (sums.getOrDefault(t.key(),BigDecimal.ZERO).compareTo(t.plannedAmount())!=0)
            invalid("tasks."+t.key()+".plannedAmount","必须等于该任务所有日安排的数量之和");
        var monthKeys=new HashSet<String>();
        for (var m:doc.months()) {
            YearMonth month;
            try { month=YearMonth.parse(m.month()); } catch (Exception ex) { invalid("months.month","月份无效"); return; }
            if (!monthKeys.add(m.month()) || month.isBefore(YearMonth.from(doc.startDate())) || month.isAfter(YearMonth.from(doc.endDate())))
                invalid("months."+m.month(),"月份重复或超出起止日期");
        }
        var weekKeys=new HashSet<LocalDate>();
        for (var w:doc.weeks()) if (!weekKeys.add(w.startDate()) || w.startDate().getDayOfWeek()!=DayOfWeek.MONDAY
                || w.startDate().isAfter(doc.endDate()) || w.startDate().plusDays(6).isBefore(doc.startDate()))
            invalid("weeks."+w.startDate(),"周起始必须是周一，不重复且与计划区间重叠");
        long monthCount=ChronoUnit.MONTHS.between(YearMonth.from(doc.startDate()),YearMonth.from(doc.endDate()))+1;
        LocalDate firstMonday=doc.startDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weekCount=ChronoUnit.WEEKS.between(firstMonday,doc.endDate())+1;
        if (monthKeys.size()!=monthCount) invalid("months","需要覆盖每个月的目标说明");
        if (weekKeys.size()!=weekCount) invalid("weeks","需要覆盖每周的目标说明");
    }
    private Long subjectId(String name) {
        return subjects.list(true).stream().filter(s -> s.getName().equals(name.trim())).map(s -> s.getId()).findFirst()
                .orElseThrow(() -> new BusinessException("UNKNOWN_SUBJECT","科目未配置或未启用："+name));
    }
    private Long unitId(String name) {
        return units.list(true).stream().filter(u -> u.getName().equals(name.trim())).map(u -> u.getId()).findFirst()
                .orElseThrow(() -> new BusinessException("UNKNOWN_UNIT","单位未配置或未启用："+name));
    }
    private static void range(LocalDate a,LocalDate b,LocalDate start,LocalDate end,String field) {
        if (a.isAfter(b) || a.isBefore(start) || b.isAfter(end)) invalid(field,"日期顺序错误或超出允许范围");
    }
    private static <T> Map<String,T> unique(List<T> values,Function<T,String> key,String field) {
        var map=new HashMap<String,T>();
        for (var v:values) if (map.put(key.apply(v),v)!=null) invalid(field,"标识重复："+key.apply(v));
        return map;
    }
    private String contentHash(PlanImportDocument doc) {
        // 去除 planId，并按语义键排序，换 id / JSON 字段顺序 / 数组顺序不能重复导入同一内容。
        var canonical=new PlanImportDocument(1,"content",doc.title(),doc.startDate(),doc.endDate(),
                doc.goals().stream().sorted(Comparator.comparing(PlanImportDocument.Goal::key)).toList(),
                doc.months().stream().sorted(Comparator.comparing(PlanImportDocument.Month::month)).toList(),
                doc.weeks().stream().sorted(Comparator.comparing(PlanImportDocument.Week::startDate)).toList(),
                doc.tasks().stream().sorted(Comparator.comparing(PlanImportDocument.Task::key))
                        .map(t -> new PlanImportDocument.Task(t.key(),t.subject(),t.unit(),t.title(),t.goalKey(),t.plannedAmount().setScale(2))).toList(),
                doc.days().stream().sorted(Comparator.comparing(PlanImportDocument.Day::date))
                        .map(d -> new PlanImportDocument.Day(d.date(),d.items().stream()
                                .sorted(Comparator.comparing(PlanImportDocument.Item::taskKey))
                                .map(i -> new PlanImportDocument.Item(i.taskKey(),i.amount().setScale(2),i.estimatedMinutes())).toList())).toList());
        return digest(json.writeValueAsString(canonical));
    }
    public static String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private static String safeMessage(Exception ex) {
        String message=ex.getMessage()==null?"字段或日期格式无效":ex.getMessage().split("\\n")[0];
        return message.substring(0,Math.min(500,message.length()));
    }
    private static void invalid(String field,String message) { throw new BusinessException("INVALID_PLAN_JSON",field+"："+message); }
}
