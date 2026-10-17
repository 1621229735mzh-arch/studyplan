package com.kaoyan.study.learning;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.*;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.*;
import com.kaoyan.study.today.service.TodayService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "app.preset-account.username=kaoyan", "app.preset-account.password=test-only-password"})
class CountedActivityIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {MySqlTestSupport.registerDataSource(r);}
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired RecordArchiveService archive;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService daily;
    @Autowired PlanWorkspaceService workspace;
    @Autowired TaskQuantityService conversion;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired TodayService today;
    @Autowired com.kaoyan.study.progress.service.CatalogService catalog;
    @LocalServerPort int port;
    Long learning,review,subject;
    LocalDate date=LocalDate.of(2026,10,16);
    String source;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8),name="词数-"+suffix,unit="词-"+suffix;
        subject=subjects.create(new SubjectRequest(name,null,null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        source="""
            {"schemaVersion":6,"planId":"%s","title":"实际词数","startDate":"2026-10-16","endDate":"2026-10-17",
             "goals":[],"months":[{"month":"2026-10","title":"本月"}],"weeks":[{"startDate":"2026-10-12","title":"本周"}],
             "tasks":[{"key":"review","subject":"%s","unit":"%s","title":"复习","plannedAmount":null,
             "activity":{"mode":"COUNT_REVIEW","flexibleMinutes":0}},
             {"key":"new","subject":"%s","unit":"%s","title":"新词","plannedAmount":null,
             "activity":{"mode":"COUNT_STUDY","flexibleMinutes":0}}],
             "days":[{"date":"2026-10-16","items":[{"taskKey":"review","amount":null},{"taskKey":"new","amount":null}]},
                     {"date":"2026-10-17","items":[{"taskKey":"review","amount":null},{"taskKey":"new","amount":null}]}]}
            """.formatted(suffix,name,unit,name,unit);
        var p=imports.preview(source);imports.confirm(new PlanImportRequest(source,p.previewToken()));
        learning=tasks.list(subject,null,null).stream().filter(t->t.getTitle().equals("新词")).findFirst().orElseThrow().getId();
        review=tasks.list(subject,null,null).stream().filter(t->t.getTitle().equals("复习")).findFirst().orElseThrow().getId();
    }
    DayLearningTaskView snapshot(Long id) {return records.dayTasks(date).stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();}
    DayProgressRequest request(Long id,int amount) {return new DayProgressRequest(date,id,100,null,snapshot(id).revision(),UUID.randomUUID().toString(),BigDecimal.valueOf(amount),null);}
    @Test void quantitiesAreIndependentAndNeverCarryToTomorrow() {
        records.recordDayProgress(request(learning,37));var r=records.recordDayProgress(request(review,120));
        assertThat(r.amount()).isNull();assertThat(r.reviewAmount()).isEqualByComparingTo("120");assertThat(r.durationMinutes()).isNull();
        assertThat(tasks.progress(learning).completedAmount()).isEqualByComparingTo("37");
        assertThat(tasks.progress(review).completedAmount()).isEqualByComparingTo("0");
        assertThat(archive.archive(date,date,subject,null).stats().amounts()).singleElement().satisfies(a->assertThat(a.amount()).isEqualByComparingTo("37"));
        assertThat(today.today(date).sections().stream().flatMap(s->s.tasks().stream()).filter(t->t.id().equals(review)&&t.kind().equals("LEARNING")).findFirst().orElseThrow().recordedAmount()).isEqualByComparingTo("120");
        assertThat(snapshot(review).completionPercent()).isEqualByComparingTo("100");
        assertThat(records.dayTasks(date.plusDays(1)).stream().filter(t->t.id().equals(review)).findFirst().orElseThrow().completionPercent()).isEqualByComparingTo("0");
        assertThat(workspace.read(date,date.plusDays(1),subject).months().getFirst().amounts()).singleElement().satisfies(a->assertThat(a.completedAmount()).isEqualByComparingTo("37"));
        var revision=snapshot(review).revision();records.delete(r.id(),r.version());
        assertThat(snapshot(review).revision()).isNotEqualTo(revision);assertThat(snapshot(review).completionPercent()).isEqualByComparingTo("0");
    }
    @Test void editsAndMultipleSessionsUpdateReviewOnly() {
        var first=records.recordDayProgress(request(review,120));
        var edited=records.update(first.id(),new StudyRecordUpdateRequest(review,null,null,date,new BigDecimal("80"),null,null,null,first.version()));
        assertThat(edited.reviewAmount()).isEqualByComparingTo("80");assertThat(edited.amount()).isNull();
        records.recordDayProgress(request(review,20));
        var group=archive.archive(date,date,subject,review).months().getFirst().weeks().getFirst().days().getFirst().groups().getFirst();
        assertThat(group.reviewAmount()).isEqualByComparingTo("100");assertThat(group.amount()).isNull();
        assertThatThrownBy(()->records.update(first.id(),new StudyRecordUpdateRequest(learning,null,null,date,BigDecimal.TEN,null,null,null,edited.version()))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.update(first.id(),new StudyRecordUpdateRequest(review,null,null,date,BigDecimal.TEN,null,null,null,first.version()))).isInstanceOf(BusinessException.class);
    }
    @Test void catalogShowsBothQuantityTasksWithoutInventingTotalsAndRecalculatesEditsAndDeletes() {
        var empty=catalog.library().activities().stream().filter(a->a.taskId().equals(review)).findFirst().orElseThrow();
        assertThat(empty.recordedAmount()).isZero();assertThat(empty.recordCount()).isZero();
        records.recordDayProgress(request(learning,37));records.recordDayProgress(request(learning,13));
        var first=records.recordDayProgress(request(review,120));records.recordDayProgress(request(review,20));
        records.create(new StudyRecordCreateRequest(review,null,null,date.plusDays(1),new BigDecimal("30"),null,null,null,UUID.randomUUID().toString()));
        var library=catalog.library();
        var study=library.activities().stream().filter(a->a.taskId().equals(learning)).findFirst().orElseThrow();
        var recap=library.activities().stream().filter(a->a.taskId().equals(review)).findFirst().orElseThrow();
        assertThat(study.recordedAmount()).isEqualByComparingTo("50");assertThat(study.learningDays()).isEqualTo(1);assertThat(study.recordCount()).isEqualTo(2);
        assertThat(recap.recordedAmount()).isEqualByComparingTo("170");assertThat(recap.learningDays()).isEqualTo(2);assertThat(recap.recordCount()).isEqualTo(3);
        assertThat(library.cumulative().stream().filter(q->q.subjectId().equals(subject)).findFirst().orElseThrow().amount()).isEqualByComparingTo("50");
        assertThat(library.courses()).noneMatch(c->c.subjectId().equals(subject));
        var changed=records.update(first.id(),new StudyRecordUpdateRequest(review,null,null,date,new BigDecimal("80"),null,null,null,first.version()));
        assertThat(catalog.library().activities().stream().filter(a->a.taskId().equals(review)).findFirst().orElseThrow().recordedAmount()).isEqualByComparingTo("130");
        records.delete(changed.id(),changed.version());
        var after=catalog.library().activities().stream().filter(a->a.taskId().equals(review)).findFirst().orElseThrow();
        assertThat(after.recordedAmount()).isEqualByComparingTo("50");assertThat(after.recordCount()).isEqualTo(2);assertThat(after.learningDays()).isEqualTo(2);
    }
    @Test void retriesConcurrencyStaleAndDeletedRequestsDoNotDoubleCount() throws Exception {
        var request=request(review,67);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->records.recordDayProgress(request));var b=pool.submit(()->records.recordDayProgress(request));
            assertThat(a.get(20,TimeUnit.SECONDS).id()).isEqualTo(b.get(20,TimeUnit.SECONDS).id());
        }
        assertThat(records.list(date,date,review,null)).hasSize(1);
        var stale=request(review,12);var row=records.list(date,date,review,null).getFirst();records.delete(row.id(),row.version());
        assertThatThrownBy(()->records.recordDayProgress(stale)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.recordDayProgress(request)).isInstanceOf(BusinessException.class);
        var freshRevision=snapshot(review).revision();
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date,review,100,null,freshRevision,request.clientToken(),new BigDecimal("67"),null))).isInstanceOf(BusinessException.class);
    }
    @Test void sameTokenCannotBeUsedForChangedQuantityOrMissingToken() {
        var first=request(review,67);records.recordDayProgress(first);
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date,review,100,null,snapshot(review).revision(),first.clientToken(),new BigDecimal("68"),null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(review,null,null,date,BigDecimal.TEN,null,null,null,null))).isInstanceOf(BusinessException.class);
        assertThat(records.list(date,date,review,null)).hasSize(1);
    }
    @Test void rejectEmptyFractionalOrNegativeQuantitiesAndOldImportVersion() {
        for(var amount:new BigDecimal[]{null,BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("1.5"),new BigDecimal("100000000")})
            assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(review,null,null,date,amount,null,null,null,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        for(var invalid:new String[]{source.replace("\"schemaVersion\":6","\"schemaVersion\":5"),source.replace("\"amount\":null","\"amount\":1"),source.replace("\"flexibleMinutes\":0","\"flexibleMinutes\":1")})
            assertThatThrownBy(()->imports.preview(invalid)).isInstanceOf(BusinessException.class);
    }
    @Test void conversionPreservesOldFactsAndRejectsStalePreview() {
        String old=source.replace("\"schemaVersion\":6","\"schemaVersion\":4").replace("\"planId\":\"","\"planId\":\"old-")
                .replace("COUNT_REVIEW","CHECK_ONLY").replace("COUNT_STUDY","TIMED_STUDY");
        var p=imports.preview(old);imports.confirm(new PlanImportRequest(old,p.previewToken()));
        var oldTask=tasks.list(subject,null,null).stream().filter(t->t.getTitle().equals("复习")&&!t.getId().equals(review)).findFirst().orElseThrow();
        records.confirmDay(new DayConfirmationRequest(date,oldTask.getId(),true,snapshot(oldTask.getId()).revision(),UUID.randomUUID().toString()));
        var original=records.list(date,date,oldTask.getId(),null).getFirst();
        var preview=conversion.preview(oldTask.getId());
        assertThat(preview.preservedRecordCount()).isEqualTo(1);
        conversion.confirm(oldTask.getId(),new TaskQuantityService.Confirm("每日复习",preview.version(),preview.workspaceRevision(),preview.mode()));
        var kept=records.get(original.id());assertThat(kept.amount()).isNull();assertThat(kept.reviewAmount()).isNull();assertThat(kept.version()).isEqualTo(original.version());
        var history=catalog.library().activities().stream().filter(a->a.taskId().equals(oldTask.getId())).findFirst().orElseThrow();
        assertThat(history.recordedAmount()).isNull();assertThat(history.unknownQuantityRecords()).isEqualTo(1);assertThat(history.recordCount()).isEqualTo(1);
        records.recordDayProgress(request(oldTask.getId(),40));
        var mixed=catalog.library().activities().stream().filter(a->a.taskId().equals(oldTask.getId())).findFirst().orElseThrow();
        assertThat(mixed.recordedAmount()).isEqualByComparingTo("40");assertThat(mixed.unknownQuantityRecords()).isEqualTo(1);assertThat(mixed.learningDays()).isEqualTo(1);
        assertThat(snapshot(oldTask.getId()).completionPercent()).isEqualByComparingTo("100");
        assertThatThrownBy(()->conversion.confirm(oldTask.getId(),new TaskQuantityService.Confirm("重复",preview.version(),preview.workspaceRevision(),preview.mode()))).isInstanceOf(BusinessException.class);
        var oldNew=tasks.create(new TaskCreateRequest(subject,null,"每日50词",oldTask.getUnitId(),new BigDecimal("100")));
        daily.addItem(date,new DailyItemCreateRequest(oldNew.getId(),new BigDecimal("50"),null,null));
        var learned=records.create(new StudyRecordCreateRequest(oldNew.getId(),null,null,date,new BigDecimal("30"),null,null,null,UUID.randomUUID().toString()));
        var oldPreview=conversion.preview(oldNew.getId());
        daily.addItem(date.plusDays(1),new DailyItemCreateRequest(oldNew.getId(),new BigDecimal("50"),null,null));
        assertThatThrownBy(()->conversion.confirm(oldNew.getId(),new TaskQuantityService.Confirm("新词",oldPreview.version(),oldPreview.workspaceRevision(),oldPreview.mode()))).isInstanceOf(BusinessException.class);
        var fresh=conversion.preview(oldNew.getId());conversion.confirm(oldNew.getId(),new TaskQuantityService.Confirm("新词",fresh.version(),fresh.workspaceRevision(),fresh.mode()));
        assertThat(records.get(learned.id()).amount()).isEqualByComparingTo("30");assertThat(tasks.get(oldNew.getId()).getPlannedAmount()).isNull();
        assertThat(daily.list(date).stream().filter(i->i.taskId().equals(oldNew.getId())).findFirst().orElseThrow().plannedAmount()).isNull();
    }
    @Test void httpRequiresAuthenticationCsrfAndAcceptsQuantity() {
        var client=new HttpTestClient("http://localhost:"+port);
        assertThat(client.get("/api/plan/tasks/"+learning+"/quantity-mode").status()).isEqualTo(401);
        var csrf=client.fetchCsrfToken();
        assertThat(client.post("/api/auth/login","{\"username\":\"kaoyan\",\"password\":\"test-only-password\"}",csrf).status()).isEqualTo(200);
        var body="{\"date\":\"%s\",\"taskId\":%d,\"completionPercent\":100,\"actualAmount\":41,\"revision\":\"%s\",\"clientToken\":\"%s\"}".formatted(date,review,snapshot(review).revision(),UUID.randomUUID());
        assertThat(client.post("/api/learning/records/day-progress",body,null).status()).isEqualTo(403);
        csrf=client.fetchCsrfToken();var saved=client.post("/api/learning/records/day-progress",body,csrf);
        assertThat(saved.status()).as(saved.body()).isEqualTo(201);assertThat(saved.number("$.reviewAmount").intValue()).isEqualTo(41);assertThat(saved.value("$.amount")).isNull();
    }
}
