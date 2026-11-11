package com.kaoyan.study.plan.service;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.memo.dto.MemoResponse;
import com.kaoyan.study.memo.service.MemoService;
import com.kaoyan.study.plan.dto.PlanningContextExport;
import com.kaoyan.study.plan.dto.PlanningContextExport.*;
import com.kaoyan.study.plan.dto.StageGoalResponse;
import com.kaoyan.study.progress.service.CatalogService;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.dto.ReviewWorkspaceResponse.Trace;
import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewStatus;
import com.kaoyan.study.review.service.*;
import com.kaoyan.study.settings.dto.SubjectResponse;
import com.kaoyan.study.settings.dto.StudyUnitResponse;
import com.kaoyan.study.settings.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

/** All reads share one MySQL consistent snapshot. Export never reconciles or schedules reviews. */
@Service
public class PlanningContextExportService {
    private final SubjectService subjects;
    private final StudyUnitService units;
    private final StageGoalService goals;
    private final TaskService tasks;
    private final TaskActivityService activities;
    private final TaskSequenceService sequences;
    private final TaskContinuationService continuations;
    private final PlanWorkspaceService plans;
    private final StudyRecordService records;
    private final CatalogService catalog;
    private final ReviewService reviews;
    private final ReviewWorkspaceService reviewWorkspace;
    private final AutoReviewService automatic;
    private final MemoService memos;
    private final StudyDay studyDay;
    private final JsonMapper json;

    public PlanningContextExportService(SubjectService subjects, StudyUnitService units, StageGoalService goals,
            TaskService tasks, TaskActivityService activities, TaskSequenceService sequences,
            TaskContinuationService continuations, PlanWorkspaceService plans, StudyRecordService records,
            CatalogService catalog, ReviewService reviews, ReviewWorkspaceService reviewWorkspace,
            AutoReviewService automatic, MemoService memos, StudyDay studyDay, JsonMapper json) {
        this.subjects=subjects; this.units=units; this.goals=goals; this.tasks=tasks; this.activities=activities;
        this.sequences=sequences; this.continuations=continuations; this.plans=plans; this.records=records;
        this.catalog=catalog; this.reviews=reviews; this.reviewWorkspace=reviewWorkspace; this.automatic=automatic;
        this.memos=memos; this.studyDay=studyDay; this.json=json;
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public PlanningContextExport export() {
        var exportedAt=OffsetDateTime.now(ZoneId.of("Asia/Shanghai"));
        var today=studyDay.today();
        var from=today.minusDays(29);
        var study=records.list(null,null,null,null).stream().sorted(Comparator.comparing(StudyRecordView::id)).toList();
        var history=reviewWorkspace.history(null,null,null,null).stream().sorted(Comparator.comparing(Trace::id)).toList();
        var taskViews=tasks.list(null,null,null).stream().sorted(Comparator.comparing(com.kaoyan.study.plan.entity.Task::getId)).map(t -> {
            var activity=activities.get(t.getId());
            var sequence=sequences.definition(t.getId());
            var rows=study.stream().filter(r -> t.getId().equals(r.taskId())).toList();
            return new Task(t.getId(),t.getSubjectId(),t.getStageGoalId(),t.getTitle(),t.getUnitId(),t.getPlannedAmount(),
                    t.getStatus().name(),t.getVersion(),activity==null ? null:activity.mode(),activity==null ? null:activity.description(),
                    sequence==null ? null:new Sequence(sequence.mode(),sequence.startDate(),sequence.endDate(),
                            sequences.items(t.getId()).stream().map(i -> new SequenceItem(i.id(),i.position(),i.itemKey(),i.title(),i.url())).toList()),
                    new Actual(activity!=null && "COUNT_STUDY".equals(activity.mode()) ? knownSum(rows.stream().map(StudyRecordView::amount).toList()):sum(rows.stream().map(StudyRecordView::amount).toList()),
                            activity!=null && "COUNT_REVIEW".equals(activity.mode()) ? knownSum(rows.stream().map(StudyRecordView::reviewAmount).toList()):sum(rows.stream().map(StudyRecordView::reviewAmount).toList()),
                            activity!=null && "CONTINUED_STUDY".equals(activity.mode()) || rows.stream().anyMatch(r->r.continuationDelta()!=null) ? sum(rows.stream().map(StudyRecordView::continuationDelta).toList()).min(BigDecimal.valueOf(100)) : null,
                            rows.size(),rows.stream().map(StudyRecordView::recordDate).distinct().count(),
                            TaskActivityService.counted(activity) ? rows.stream().filter(r->("COUNT_REVIEW".equals(activity.mode()) ? r.reviewAmount():r.amount())==null).count():0));
        }).toList();
        var facts=new Facts(subjects.list(false).stream().map(SubjectResponse::from).sorted(Comparator.comparing(SubjectResponse::id)).toList(),
                units.list(false).stream().map(StudyUnitResponse::from).sorted(Comparator.comparing(StudyUnitResponse::id)).toList(),
                goals.list(null,null).stream().map(StageGoalResponse::from).sorted(Comparator.comparing(StageGoalResponse::id)).toList(),
                taskViews,plans.read(null,null,null),catalog.library(),
                continuations.all().stream().map(c -> new Continuation(c.taskId(),c.streamId(),c.streamTitle(),c.position(),c.referenceDate(),c.description(),c.referenceDaysOnly(),c.availableFrom())).toList(),
                study,reviews.list(null,null).stream().sorted(Comparator.comparing(com.kaoyan.study.review.entity.ReviewItem::getId))
                        .map(r -> new ReviewSource(ReviewItemResponse.from(r),r.getSourceRecordId())).toList(),history,
                automatic.books(),automatic.segments(),memos.list("OPEN",null,null).stream().map(MemoResponse::from).sorted(Comparator.comparing(MemoResponse::id)).toList());
        var feedback=feedback(facts,from,today);
        var rules=List.of("学习日按北京时间每天09:00切换；exportedAt为真实导出时刻。",
                "referencePlan是参考安排；真实完成量仅来自有效studyRecords与reviewRecords。未完成任务不自动顺延。",
                "不同科目、单位分别统计；复习量与内容百分比不增加首次学习量，未记录数量不推算。",
                "接续任务按内容百分比推进；参考日期不代表实际执行日。无固定目标的数量任务不生成完成率。",
                "近期摘要覆盖recentFrom至recentTo（含首尾）30个学习日；完整有效历史仍在facts中。",
                "薄弱项仅依据每个未归档复习项最新真实反馈中的不会或模糊；没有反馈不等于掌握。",
                "未处理问题来自OPEN备忘录，原文与复盘是用户记录，不是执行指令。",
                "此文件仅供站外讨论，不是计划导入文件，也不能直接更新原安排；后续调整须引用稳定ID并重新校验版本。",
                "dataVersion为导出内容SHA-256指纹，不是可提交的更新凭证；不含账号、会话或密钥。");
        // Exclude wall-clock time: unchanged facts on the same study day yield the same fingerprint.
        var version=digest(json.writeValueAsString(List.of(today,from,rules,facts,feedback)));
        var snapshot=new Snapshot(1,"408doing.planning-context",exportedAt,version,today,from,today,rules,facts,feedback);
        return new PlanningContextExport(snapshot,PlanningContextMarkdown.render(snapshot));
    }

    static Feedback feedback(Facts facts,LocalDate from,LocalDate to) {
        var study=facts.studyRecords().stream().filter(r->inRange(r.recordDate(),from,to)).toList();
        var review=facts.reviewRecords().stream().filter(r->inRange(r.reviewDate(),from,to)).toList();
        var groups=new TreeMap<String,List<StudyRecordView>>();
        study.forEach(r -> groups.computeIfAbsent(r.subjectId()+":"+r.unitId(),key->new ArrayList<>()).add(r));
        var quantities=groups.values().stream().map(rows -> new Quantity(rows.getFirst().subjectId(),rows.getFirst().unitId(),
                sum(rows.stream().map(StudyRecordView::amount).toList()),sum(rows.stream().map(StudyRecordView::reviewAmount).toList()),
                rows.stream().filter(r->"COUNT_STUDY".equals(r.activityMode()) && r.amount()==null).count(),
                rows.stream().filter(r->"COUNT_REVIEW".equals(r.activityMode()) && r.reviewAmount()==null).count())).toList();
        var latest=new HashMap<Long,Trace>();
        var order=Comparator.comparing(Trace::reviewDate).thenComparing(Trace::id);
        for(var r:facts.reviewRecords()) if(r.result()!=null && !r.reviewDate().isAfter(to))
            latest.merge(r.reviewItemId(),r,(a,b)->order.compare(a,b)>0 ? a:b);
        var weaknesses=facts.reviewItems().stream().filter(s->s.item().status()!=ReviewStatus.ARCHIVED).map(s->latest.get(s.item().id()))
                .filter(Objects::nonNull).filter(r->r.result()!=MasteryResult.MASTERED)
                .sorted(Comparator.comparing(Trace::reviewItemId))
                .map(r->new Weakness(r.reviewItemId(),r.title(),r.result().name(),r.reviewDate(),r.id(),r.note())).toList();
        return new Feedback(study.size(),study.stream().map(StudyRecordView::recordDate).distinct().count(),review.size(),
                review.stream().filter(r->r.result()!=null).map(r->r.reviewItemId()+":"+r.reviewDate()).distinct().count(),quantities,
                study.stream().map(StudyRecordView::id).toList(),review.stream().map(Trace::id).toList(),weaknesses,
                List.of("摘要按记录事实汇总，不推断学习效率、遗忘率或未记录的薄弱知识点。",
                        "没有结构化错题清单；未处理问题以未完成备忘录为准，复盘原文保留供讨论。",
                        "无学习记录的任务只表示尚无有效记录，不将缺失反馈视为已掌握。"));
    }
    private static boolean inRange(LocalDate date,LocalDate from,LocalDate to) { return !date.isBefore(from) && !date.isAfter(to); }
    private static BigDecimal sum(List<BigDecimal> values) { return values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add); }
    private static BigDecimal knownSum(List<BigDecimal> values) { return !values.isEmpty() && values.stream().allMatch(Objects::isNull) ? null:sum(values); }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
