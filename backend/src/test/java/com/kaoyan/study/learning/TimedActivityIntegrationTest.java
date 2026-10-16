package com.kaoyan.study.learning;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class TimedActivityIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) { MySqlTestSupport.registerDataSource(r); }
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService daily;
    @Autowired PlanWorkspaceService workspace;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired com.kaoyan.study.today.service.TodayService today;
    Long study,recap,subjectId;
    String source;
    LocalDate date=LocalDate.of(2026,10,15);
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        String subject="数学-"+suffix,unit="题-"+suffix;
        subjectId=subjects.create(new SubjectRequest(subject,null,null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        source="""
          {"schemaVersion":3,"planId":"%s","title":"限时高数","startDate":"2026-10-15","endDate":"2026-10-15",
           "goals":[],"months":[{"month":"2026-10","title":"本月"}],"weeks":[{"startDate":"2026-10-12","title":"本周"}],
           "tasks":[{"key":"learn","subject":"%s","unit":"%s","title":"新学练习","plannedAmount":null,
             "activity":{"mode":"TIMED_STUDY","description":"首次题量据实","flexibleMinutes":30}},
             {"key":"recap","subject":"%s","unit":"%s","title":"复盘","plannedAmount":null,
             "activity":{"mode":"RECAP","description":"不重复计量","flexibleMinutes":0}}],
           "days":[{"date":"2026-10-15","items":[{"taskKey":"learn","amount":null,"estimatedMinutes":100},
                    {"taskKey":"recap","amount":null,"estimatedMinutes":20}]}]}
          """.formatted(suffix,subject,unit,subject,unit);
        var preview=imports.preview(source);
        assertThat(tasks.list(subjectId,null,null)).isEmpty();
        imports.confirm(new PlanImportRequest(source,preview.previewToken()));
        var all=tasks.list(subjectId,null,null);
        study=all.stream().filter(t->t.getTitle().equals("新学练习")).findFirst().orElseThrow().getId();
        recap=all.stream().filter(t->t.getTitle().equals("复盘")).findFirst().orElseThrow().getId();
    }
    com.kaoyan.study.learning.dto.DayLearningTaskView snapshot(Long task) {
        return records.dayTasks(date).stream().filter(t->t.id().equals(task)).findFirst().orElseThrow();
    }
    DayProgressRequest request(Long task,int percent,int minutes,BigDecimal amount,String token) {
        return new DayProgressRequest(date,task,percent,minutes,snapshot(task).revision(),token,amount,"教材p12；基础篇第5题；卡在条件");
    }
    @Test void actualQuantityIsIndependentOfTimeAndDeletingReopensActivity() {
        var initialCounts=today.today(date).taskCounts();
        var first=records.recordDayProgress(request(study,0,40,new BigDecimal("3"),UUID.randomUUID().toString()));
        var startedCounts=new com.kaoyan.study.today.dto.TodayTaskCounts(
                initialCounts.completed(),initialCounts.inProgress()+1,initialCounts.remaining()-1);
        assertThat(today.today(date).taskCounts()).isEqualTo(startedCounts);
        assertThat(first.amount()).isEqualByComparingTo("3");
        assertThat(snapshot(study).plannedAmount()).isNull();
        assertThat(snapshot(study).completionPercent()).isEqualByComparingTo("0");
        assertThat(snapshot(study).remainingMinutes()).isEqualByComparingTo("60");
        var req=request(study,100,50,new BigDecimal("2"),UUID.randomUUID().toString());
        var done=records.recordDayProgress(req);
        assertThat(records.recordDayProgress(req).id()).isEqualTo(done.id());
        assertThat(today.today(date).taskCounts()).isEqualTo(new com.kaoyan.study.today.dto.TodayTaskCounts(
                initialCounts.completed()+1,initialCounts.inProgress(),initialCounts.remaining()-1));
        assertThat(snapshot(study).completionPercent()).isEqualByComparingTo("100");
        assertThat(snapshot(study).remainingMinutes()).isEqualByComparingTo("0");
        assertThat(tasks.progress(study).completedAmount()).isEqualByComparingTo("5");
        records.delete(done.id(),done.version());
        assertThat(today.today(date).taskCounts()).isEqualTo(startedCounts);
        assertThat(snapshot(study).completionPercent()).isEqualByComparingTo("0");
        assertThat(tasks.progress(study).completedAmount()).isEqualByComparingTo("3");
    }
    @Test void recapCannotAddQuantityViaEitherRecordEntryOrEdit() {
        assertThatThrownBy(()->records.recordDayProgress(request(recap,100,20,BigDecimal.ONE,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        var record=records.recordDayProgress(request(recap,100,20,null,UUID.randomUUID().toString()));
        assertThat(record.amount()).isNull();
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(recap,null,null,date,BigDecimal.ONE,10,null,null,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.update(record.id(),new StudyRecordUpdateRequest(recap,null,null,date,BigDecimal.ONE,20,null,null,record.version()))).isInstanceOf(BusinessException.class);
        assertThat(tasks.progress(recap).completedAmount()).isEqualByComparingTo("0");
    }
    @Test void staleDevicesAndInventedPercentagesAreRejected() {
        var stale=request(study,100,20,null,UUID.randomUUID().toString());
        records.recordDayProgress(request(study,0,10,null,UUID.randomUUID().toString()));
        assertThatThrownBy(()->records.recordDayProgress(stale)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.recordDayProgress(request(study,50,20,null,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        assertThat(records.recordDayProgress(request(study,100,0,null,UUID.randomUUID().toString())).durationMinutes()).isZero();
    }
    @Test void timeOnlyWorkspaceDoesNotInventQuantityAndAllowsBudgetEdit() {
        var result=workspace.read(date,date,subjectId);
        assertThat(result.entries()).hasSize(2).allSatisfy(e->assertThat(e.plannedAmount()).isNull());
        assertThat(result.months().getFirst().amounts().getFirst().plannedAmount()).isNull();
        var item=daily.list(date).stream().filter(i->i.taskId().equals(study)).findFirst().orElseThrow();
        daily.adjust(date,item.id(),new DailyItemAdjustRequest(null,item.version(),"校准用时",90));
        assertThat(snapshot(study).estimatedMinutes()).isEqualTo(90);
        var changed=daily.list(date).stream().filter(i->i.taskId().equals(study)).findFirst().orElseThrow();
        assertThatThrownBy(()->daily.adjust(date,changed.id(),new DailyItemAdjustRequest(BigDecimal.ONE,changed.version(),"伪造题数",90))).isInstanceOf(BusinessException.class);
    }
    @Test void strictImportKeepsLegacyQuantitiesRequiredAndActivityMetadataTyped() {
        for (String invalid:new String[]{source.replace("\"schemaVersion\":3","\"schemaVersion\":1"),
                source.replace("\"flexibleMinutes\":30","\"flexibleMinutes\":\"30\""),
                source.replace("\"amount\":null","\"amount\":1"),
                source.replace("\"estimatedMinutes\":100","\"estimatedMinutes\":null"),
                source.replace("TIMED_STUDY","OTHER")})
            assertThatThrownBy(()->imports.preview(invalid)).isInstanceOf(BusinessException.class);
    }

    @Test void canEndStudyAndRecapWithoutRecordingTime() {
        for (var id:java.util.List.of(study,recap)) {
            var req=new DayProgressRequest(date,id,100,null,snapshot(id).revision(),UUID.randomUUID().toString(),null,null);
            var saved=records.recordDayProgress(req);
            assertThat(saved.durationMinutes()).isNull();
            assertThat(saved.amount()).isNull();
            assertThat(records.recordDayProgress(req).id()).isEqualTo(saved.id());
            assertThat(snapshot(id).completionPercent()).isEqualByComparingTo("100");
            records.delete(saved.id(),saved.version());
            assertThat(snapshot(id).completionPercent()).isEqualByComparingTo("0");
        }
    }
    @Test void importedActivitiesDoNotRequireEstimates() {
        var withoutTime=source.replaceAll("\"estimatedMinutes\":(100|20)","\"estimatedMinutes\":null")
                .replaceAll("\"flexibleMinutes\":(30|0)","\"flexibleMinutes\":null");
        withoutTime=withoutTime.replaceFirst("\"planId\":\"[^\"]+\"", "\"planId\":\""+UUID.randomUUID()+"\"");
        var preview=imports.preview(withoutTime);
        assertThat(preview).isNotNull();
        imports.confirm(new PlanImportRequest(withoutTime,preview.previewToken()));
        assertThat(daily.list(date).stream().filter(p->p.subjectId().equals(subjectId)).filter(p->!p.taskId().equals(study) && !p.taskId().equals(recap)).toList()).hasSize(2).allSatisfy(p->assertThat(p.estimatedMinutes()).isNull());
        var item=daily.list(date).stream().filter(p->p.taskId().equals(study)).findFirst().orElseThrow();
        daily.adjust(date,item.id(),new DailyItemAdjustRequest(null,item.version(),"无需计时",null));
        assertThat(snapshot(study).estimatedMinutes()).isNull();
    }

}
