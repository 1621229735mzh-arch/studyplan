package com.kaoyan.study.plan.service;

import com.kaoyan.study.plan.dto.PlanningContextExport.*;
import java.util.*;

/** Human-readable rendering of the exact snapshot returned with the JSON export. */
final class PlanningContextMarkdown {
    private PlanningContextMarkdown() {}
    static String render(Snapshot s) {
        var b=new StringBuilder("# 外部规划上下文\n\n");
        b.append("导出时点：").append(s.exportedAt()).append("\n\n学习日：").append(s.studyDate())
                .append("（北京时间09:00切换）\n\n数据版本：`").append(s.dataVersion())
                .append("`\n\n格式：408doing.planning-context v").append(s.schemaVersion()).append("\n\n");
        section(b,"讨论边界"); s.rules().forEach(rule->b.append("- ").append(rule).append('\n')); b.append('\n');
        var f=s.facts(); var summary=s.feedback();
        section(b,"最近学习与复习反馈");
        b.append(s.recentFrom()).append(" — ").append(s.recentTo()).append("（含首尾）\n\n")
                .append("学习记录 ").append(summary.studyRecordCount()).append(" 次，学习 ").append(summary.learningDays())
                .append(" 天；复习记录 ").append(summary.reviewRecordCount()).append(" 次，完成复习 ").append(summary.completedReviews()).append(" 次。\n\n");
        table(b,"科目ID","单位ID","已记录首次学习量","已记录复习量","新学数量未知次数","复习数量未知次数");
        summary.quantities().forEach(q->row(b,q.subjectId(),q.unitId(),q.firstStudyAmount(),q.reviewAmount(),q.unknownFirstStudyRecords(),q.unknownReviewRecords())); b.append('\n');
        b.append("近期学习记录ID：").append(summary.studyRecordIds()).append("\n\n近期复习记录ID：").append(summary.reviewRecordIds()).append("\n\n");
        summary.limitations().forEach(l->b.append("- ").append(l).append('\n')); b.append('\n');
        section(b,"薄弱项（最新真实反馈）");
        if(summary.weaknesses().isEmpty()) b.append("没有可据真实反馈列出的薄弱项；不代表全部掌握。\n\n");
        else { table(b,"复习项ID","内容","反馈","反馈日期","证据记录ID","复盘");
            summary.weaknesses().forEach(w->row(b,w.reviewItemId(),w.title(),mastery(w.result()),w.feedbackDate(),w.evidenceRecordId(),w.note())); b.append('\n'); }
        section(b,"未处理问题（OPEN备忘录）");
        if(f.openMemos().isEmpty()) b.append("无未完成备忘录；没有另建错题或问题清单。\n\n");
        else { table(b,"备忘录ID","科目ID","内容","到期日","已转任务ID","版本");
            f.openMemos().forEach(m->row(b,m.id(),m.subjectId(),m.content(),m.dueDate(),m.convertedTaskId(),m.version())); b.append('\n'); }
        section(b,"科目与单位（含停用项）"); table(b,"科目ID","名称","分类","启用");
        f.subjects().forEach(x->row(b,x.id(),x.name(),x.category(),x.enabled())); b.append('\n');
        table(b,"单位ID","名称","启用"); f.units().forEach(x->row(b,x.id(),x.name(),x.enabled())); b.append('\n');
        section(b,"阶段目标"); table(b,"目标ID","科目ID","标题","说明","起始","目标日期","状态","版本");
        f.goals().forEach(g->row(b,g.id(),g.subjectId(),g.title(),g.description(),g.startDate(),g.targetDate(),g.status(),g.version())); b.append('\n');
        section(b,"课程与完整目录（真实进度）");
        for(var c:f.progress().courses()) {
            b.append("### ").append(cell(c.title())).append(" · 课程ID ").append(c.id()).append('\n').append('\n');
            table(b,"课程key","方向","分科","科目ID","单位ID","进度模式");
            row(b,c.key(),c.direction(),c.branch(),c.subjectId(),c.unitId(),c.progressMode());
            b.append("\n总量：").append(cell(c.totalAmount())).append("；完成量：").append(cell(c.completedAmount()))
                    .append("；记录量：").append(cell(c.recordedAmount())).append(' ').append(cell(c.unitName())).append("。\n\n");
            if(c.contentCounts()!=null) b.append("内容项：总计 ").append(c.contentCounts().total()).append(" / 已完成 ").append(c.contentCounts().completed())
                    .append(" / 进行中 ").append(c.contentCounts().inProgress()).append(" / 未开始 ").append(c.contentCounts().remaining()).append("。\n\n");
            b.append("已完成题号：").append(c.completedProblemNumbers()).append("\n\n");
            table(b,"节点ID","key","父key","内容","总量","完成量","内容百分比","任务ID","顺序");
            c.nodes().forEach(n->row(b,n.id(),n.key(),n.parentKey(),n.title(),n.totalAmount(),n.completedAmount(),n.completionPercent(),n.taskIds(),n.sortOrder())); b.append('\n');
        }
        table(b,"方向","科目ID","科目","单位ID","单位","累计首次学习量");
        f.progress().cumulative().forEach(q->row(b,q.direction(),q.subjectId(),q.subjectName(),q.unitId(),q.unitName(),q.amount())); b.append('\n');
        b.append("未归类学习记录ID：").append(f.progress().unclassified().stream().map(r->r.id()).toList()).append("\n\n");
        section(b,"任务与真实完成情况"); table(b,"任务ID","科目ID","目标ID","内容","单位ID","参考总量","状态","模式","首次学习量","复习量","累计内容百分比","记录次数","学习天数","数量未知次数","版本","说明");
        f.tasks().forEach(t->row(b,t.id(),t.subjectId(),t.stageGoalId(),t.title(),t.unitId(),t.plannedAmount(),t.status(),t.activityMode(),
                t.actual().firstStudyAmount(),t.actual().reviewAmount(),t.actual().contentPercent(),t.actual().recordCount(),t.actual().learningDays(),t.actual().unknownQuantityRecords(),t.version(),t.description())); b.append('\n');
        section(b,"按书接续元数据"); table(b,"任务ID","教材ID","教材","顺序","参考日期","说明");
        f.continuations().forEach(c->row(b,c.taskId(),c.streamId(),c.bookTitle(),c.position(),c.referenceDate(),c.description())); b.append('\n');
        section(b,"题目清单与模式");
        for(var t:f.tasks()) if(t.sequence()!=null) {
            b.append("任务ID ").append(t.id()).append(" · ").append(t.sequence().mode()).append(" · ").append(t.sequence().startDate()).append(" — ").append(t.sequence().endDate()).append("\n\n");
            table(b,"题目ID","顺序","key","题名／题号","链接"); t.sequence().items().forEach(i->row(b,i.id(),i.position(),i.key(),i.title(),i.url())); b.append('\n');
        }
        section(b,"原参考安排"); b.append("工作区版本：").append(f.referencePlan().revision()).append("\n\n");
        table(b,"方案ID","标题","起始日期","结束日期");
        f.referencePlan().imports().forEach(p->row(b,p.planKey(),p.title(),p.startDate(),p.endDate())); b.append('\n');
        table(b,"纲要类型","日期","标题","说明"); f.referencePlan().outlines().forEach(o->row(b,o.periodType(),o.startDate(),o.title(),o.description())); b.append('\n');
        table(b,"参考日期","任务ID","内容","科目ID","计划量","当日真实首次学习量","单位","模式","来源","原预计分钟","顺序");
        f.referencePlan().entries().forEach(e->row(b,e.date(),e.taskId(),e.taskTitle(),e.subjectId(),e.plannedAmount(),e.completedAmount(),e.unitName(),e.activityMode(),e.source(),e.estimatedMinutes(),e.sortOrder())); b.append('\n');
        section(b,"完整有效学习记录"); table(b,"记录ID","任务ID","科目ID","单位ID","学习日","首次学习量","复习量","内容百分比增量","学习内容","复盘","版本","题目ID","题名／题号","链接","模式","历史分钟");
        f.studyRecords().forEach(r->row(b,r.id(),r.taskId(),r.subjectId(),r.unitId(),r.recordDate(),r.amount(),r.reviewAmount(),r.continuationDelta(),r.content(),r.note(),r.version(),r.sequenceItemId(),r.sequenceTitle(),r.sequenceUrl(),r.activityMode(),r.durationMinutes())); b.append('\n');
        section(b,"复习项与来源"); table(b,"复习项ID","任务ID","来源记录ID","科目ID","内容","状态","下次到期","已安排日期","最近反馈","间隔天数","版本");
        f.reviewItems().forEach(x->{var r=x.item(); row(b,r.id(),r.taskId(),x.sourceRecordId(),r.subjectId(),r.title(),r.status(),r.nextReviewDate(),r.scheduledDate(),mastery(r.lastResult()==null?null:r.lastResult().name()),r.intervalDays(),r.version());}); b.append('\n');
        section(b,"完整复习反馈记录"); table(b,"记录ID","复习项ID","任务ID","科目ID","内容","学习日","进度增量","反馈","复盘","历史分钟");
        f.reviewRecords().forEach(r->row(b,r.id(),r.reviewItemId(),r.taskId(),r.subjectId(),r.title(),r.reviewDate(),r.progressDelta(),mastery(r.result()==null?null:r.result().name()),r.note(),r.durationMinutes())); b.append('\n');
        section(b,"教材自动复习状态"); table(b,"教材ID","科目ID","教材","启用","版本","内容段数","复习项数","逾期数","暂停数");
        f.automaticReviewBooks().forEach(x->row(b,x.streamId(),x.subjectId(),x.title(),x.enabled(),x.version(),x.segmentCount(),x.reviewCount(),x.overdueCount(),x.suspendedCount())); b.append('\n');
        table(b,"复习项ID","教材ID","内容","真实完成日","来源有效","暂停","教材启用","状态","已安排日期","下次日期","反馈轮数","版本");
        f.automaticReviewSegments().forEach(x->row(b,x.reviewItemId(),x.streamId(),x.title(),x.completedOn(),x.sourceValid(),x.paused(),x.enabled(),x.status(),x.scheduledDate(),x.nextReviewDate(),x.repetitions(),x.version()));
        return b.toString();
    }
    private static void section(StringBuilder b,String title) { b.append("## ").append(title).append("\n\n"); }
    private static void table(StringBuilder b,String... headings) { row(b,(Object[])headings); row(b,Arrays.stream(headings).map(h->"---").toArray()); }
    private static void row(StringBuilder b,Object... values) { b.append("| "); for(var v:values) b.append(cell(v)).append(" | "); b.append('\n'); }
    static String cell(Object value) {
        if(value==null) return "未记录";
        return value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\\","&#92;").replace("|","&#124;").replace("`","&#96;").replace("*","&#42;")
                .replace("[","&#91;").replace("]","&#93;").replace("_","&#95;").replace("\r\n","\n").replace("\r","\n").replace("\n","<br>");
    }
    private static String mastery(String result) { return result==null ? null:switch(result) { case "FORGOT"->"不会"; case "VAGUE"->"模糊"; case "MASTERED"->"掌握"; default->result; }; }
}
