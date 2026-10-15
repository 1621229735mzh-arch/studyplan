package com.kaoyan.study.learning;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.*;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.today.service.TodayService;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="app.preset-account.password=confirmation-test-only")
class ConfirmationActivityIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {MySqlTestSupport.registerDataSource(r);}
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired RecordArchiveService archive;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService daily;
    @Autowired PlanWorkspaceService workspace;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired TodayService today;
    Long task,subject;
    LocalDate date=LocalDate.of(2026,10,15);
    String source;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8),name="英语一-"+suffix,unit="词-"+suffix;
        subject=subjects.create(new SubjectRequest(name,null,null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        source="""
            {"schemaVersion":4,"planId":"%s","title":"每日词汇","startDate":"2026-10-15","endDate":"2026-10-16",
             "goals":[],"months":[{"month":"2026-10","title":"本月"}],"weeks":[{"startDate":"2026-10-12","title":"本周"}],
             "tasks":[{"key":"review","subject":"%s","unit":"%s","title":"单词复习","plannedAmount":null,
             "activity":{"mode":"CHECK_ONLY","flexibleMinutes":0}},
             {"key":"new","subject":"%s","unit":"%s","title":"每日新词50","plannedAmount":100}],
             "days":[{"date":"2026-10-15","items":[{"taskKey":"review","amount":null,"estimatedMinutes":null},{"taskKey":"new","amount":50}]},
                     {"date":"2026-10-16","items":[{"taskKey":"review","amount":null},{"taskKey":"new","amount":50}]}]}
            """.formatted(suffix,name,unit,name,unit);
        var preview=imports.preview(source);
        imports.confirm(new PlanImportRequest(source,preview.previewToken()));
        task=tasks.list(subject,null,null).stream().filter(t->t.getTitle().equals("单词复习")).findFirst().orElseThrow().getId();
    }
    DayLearningTaskView snapshot() {return records.dayTasks(date).stream().filter(t->t.id().equals(task)).findFirst().orElseThrow();}
    DayConfirmationRequest request(boolean complete) {return new DayConfirmationRequest(date,task,complete,snapshot().revision(),UUID.randomUUID().toString());}
    @Test void confirmCancelRetryAndNextDayAreIndependent() {
        var initial=request(true);
        var done=records.confirmDay(initial);
        assertThat(done.completionPercent()).isEqualByComparingTo("100");
        records.confirmDay(initial);
        assertThat(records.list(date,date,task,null)).singleElement().satisfies(r->{assertThat(r.amount()).isNull();assertThat(r.durationMinutes()).isNull();assertThat(r.activityMode()).isEqualTo("CHECK_ONLY");});
        assertThat(tasks.progress(task).completedAmount()).isEqualByComparingTo("0");
        assertThat(records.dayTasks(date.plusDays(1)).stream().filter(t->t.id().equals(task)).findFirst().orElseThrow().completionPercent()).isEqualByComparingTo("0");
        var cancel=request(false);
        records.confirmDay(cancel);records.confirmDay(cancel);
        assertThat(records.list(date,date,task,null)).isEmpty();
        assertThat(snapshot().completionPercent()).isEqualByComparingTo("0");
        assertThat(snapshot().revision()).isNotEqualTo(initial.revision());
        assertThatThrownBy(()->records.confirmDay(initial)).isInstanceOf(BusinessException.class);
        records.confirmDay(request(true));
        assertThatThrownBy(()->records.confirmDay(cancel)).isInstanceOf(BusinessException.class);
    }
    @Test void concurrentConfirmationHasOnlyOneActiveRecord() throws Exception {
        var one=request(true);var two=request(true);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->records.confirmDay(one));var b=pool.submit(()->records.confirmDay(two));
            assertThat(a.get(15,TimeUnit.SECONDS).completionPercent()).isEqualByComparingTo("100");
            assertThat(b.get(15,TimeUnit.SECONDS).completionPercent()).isEqualByComparingTo("100");
        }
        assertThat(records.list(date,date,task,null)).hasSize(1);
    }
    @Test void confirmationDoesNotPolluteQuantityTimeOrMissingTime() {
        records.confirmDay(request(true));
        var stats=archive.archive(date,date,subject,null).stats();
        assertThat(stats.amounts()).isEmpty();assertThat(stats.studyMinutes()).isNull();assertThat(stats.missingDurationRecords()).isZero();
        var section=today.today(date).sections().stream().filter(s->s.tasks().stream().anyMatch(t->t.id().equals(task))).findFirst().orElseThrow();
        assertThat(section.actualMinutes()).isZero();assertThat(section.missingDurations()).isZero();
        assertThat(section.missingEstimates()).isEqualTo(1); // only the new-word task has an unknown budget
        var month=workspace.read(date,date.plusDays(1),subject).months().getFirst();
        assertThat(month.confirmationCount()).isEqualTo(2);
        assertThat(month.amounts()).singleElement().satisfies(a->assertThat(a.plannedAmount()).isEqualByComparingTo("100"));
        var record=records.list(date,date,task,null).getFirst();records.delete(record.id(),record.version());
        assertThat(snapshot().completionPercent()).isEqualByComparingTo("0");
    }
    @Test void otherWritePathsCannotInventLearningOrTime() {
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(task,null,null,date,BigDecimal.ONE,0,null,null,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date,task,100,0,snapshot().revision(),UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        records.confirmDay(request(true));var record=records.list(date,date,task,null).getFirst();
        assertThatThrownBy(()->records.update(record.id(),new StudyRecordUpdateRequest(task,null,null,date,BigDecimal.ONE,5,null,null,record.version()))).isInstanceOf(BusinessException.class);
        var item=daily.list(date).stream().filter(t->t.taskId().equals(task)).findFirst().orElseThrow();
        assertThatThrownBy(()->daily.adjust(date,item.id(),new DailyItemAdjustRequest(null,item.version(),"虚构时间",1))).isInstanceOf(BusinessException.class);
    }
    @Test void importRejectsWrongVersionInventedQuantityTimeAndFlexibility() {
        for(String invalid:new String[]{source.replace("\"schemaVersion\":4","\"schemaVersion\":3"),source.replace("\"estimatedMinutes\":null","\"estimatedMinutes\":1"),source.replace("\"amount\":null","\"amount\":1"),source.replace("\"flexibleMinutes\":0","\"flexibleMinutes\":1")})
            assertThatThrownBy(()->imports.preview(invalid)).isInstanceOf(BusinessException.class);
    }
}
