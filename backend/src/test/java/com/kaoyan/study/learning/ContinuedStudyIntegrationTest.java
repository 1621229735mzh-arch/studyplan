package com.kaoyan.study.learning;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import com.kaoyan.study.today.service.TodayService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "app.preset-account.username=kaoyan", "app.preset-account.password=test-only-password"})
class ContinuedStudyIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) { MySqlTestSupport.registerDataSource(r); }
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService daily;
    @Autowired TodayService today;
    @Autowired PlanWorkspaceService workspace;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    LocalDate start=LocalDate.of(2026,10,15);
    Long subjectId;
    String source;
    Map<String,Long> ids;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        String subject="数接续-"+suffix,unit="题-"+suffix;
        subjectId=subjects.create(new SubjectRequest(subject,null,null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        var entries=new ArrayList<String>(); var days=new ArrayList<String>();
        for (int i=1;i<=3;i++) {
            for (String book:List.of("教材","练习")) entries.add("""
                {"key":"%s%d","subject":"%s","unit":"%s","title":"%s%d","plannedAmount":null,
                 "activity":{"mode":"CONTINUED_STUDY","description":"%s具体学习内容%d","flexibleMinutes":0},
                 "continuation":{"streamKey":"%s","title":"%s","position":%d}}
                """.formatted(book.equals("教材")?"book":"practice",i,subject,unit,book,i,book,i,book.equals("教材")?"book":"practice",book,i));
            days.add("""
                {"date":"%s","items":[{"taskKey":"book%d","amount":null,"estimatedMinutes":%d},
                  {"taskKey":"practice%d","amount":null,"estimatedMinutes":%d}]}
                """.formatted(start.plusDays(i-1),i,i==2?60:75,i,i==2?60:45));
        }
        source="""
            {"schemaVersion":5,"planId":"continued-%s","title":"高数接续","startDate":"2026-10-15","endDate":"2026-10-17",
             "goals":[],"months":[{"month":"2026-10","title":"本月"}],"weeks":[{"startDate":"2026-10-12","title":"本周"}],
             "tasks":[%s],"days":[%s]}
            """.formatted(suffix,String.join(",",entries),String.join(",",days));
        var preview=imports.preview(source);
        imports.confirm(new PlanImportRequest(source,preview.previewToken()));
        ids=new HashMap<>();tasks.list(subjectId,null,null).forEach(t->ids.put(t.getTitle(),t.getId()));
    }
    List<DayLearningTaskView> snapshots(LocalDate date) { return records.dayTasks(date).stream().filter(t->t.subjectId().equals(subjectId)).toList(); }
    DayLearningTaskView snapshot(String title,LocalDate date) { return snapshots(date).stream().filter(t->t.title().equals(title)).findFirst().orElseThrow(); }
    DayProgressRequest request(String title,LocalDate date,int percent,int minutes) {
        var task=snapshot(title,date);
        return new DayProgressRequest(date,task.id(),percent,minutes,task.revision(),UUID.randomUUID().toString());
    }
    @Test void taskCountsUseConcreteContentsAndCarriedPartialProgress() {
        var baseline=today.today(start).taskCounts();
        var tomorrow=today.today(start.plusDays(1)).taskCounts();
        records.recordDayProgress(request("教材1",start,40,10));
        assertThat(today.today(start).taskCounts()).isEqualTo(new com.kaoyan.study.today.dto.TodayTaskCounts(
                baseline.completed(),baseline.inProgress()+1,baseline.remaining()-1));
        // One carried partial content and one newly admitted content, rather than one book group.
        assertThat(today.today(start.plusDays(1)).taskCounts()).isEqualTo(new com.kaoyan.study.today.dto.TodayTaskCounts(
                tomorrow.completed(),tomorrow.inProgress()+1,tomorrow.remaining()));
        var done=records.recordDayProgress(request("教材1",start.plusDays(1),100,10));
        assertThat(today.today(start.plusDays(1)).taskCounts()).isEqualTo(new com.kaoyan.study.today.dto.TodayTaskCounts(
                tomorrow.completed()+1,tomorrow.inProgress(),tomorrow.remaining()));
        records.delete(done.id(),done.version());
        assertThat(today.today(start.plusDays(1)).taskCounts()).isEqualTo(new com.kaoyan.study.today.dto.TodayTaskCounts(
                tomorrow.completed(),tomorrow.inProgress()+1,tomorrow.remaining()));
    }
    @Test void zeroDaysDoNotAccumulateAndEndDateIsOnlyAReference() {
        assertThat(snapshots(start.plusDays(40))).extracting(DayLearningTaskView::title).containsExactly("教材1","练习1");
        records.recordDayProgress(request("教材1",start,0,30));
        assertThat(snapshots(start.plusDays(1))).extracting(DayLearningTaskView::title).containsExactly("教材1","练习1");
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(start,ids.get("教材2"),10,1,"stale",UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class);
    }
    @Test void finishingOnlyOldContentDefersTheNewContentAndKeepsBooksIndependent() {
        records.recordDayProgress(request("教材1",start,40,30));
        assertThat(snapshot("教材1",start.plusDays(1)).remainingMinutes()).isEqualByComparingTo("45");
        assertThat(snapshots(start.plusDays(1))).extracting(DayLearningTaskView::title).containsExactly("教材1","教材2","练习1");
        records.recordDayProgress(request("教材1",start.plusDays(1),100,25));
        assertThat(snapshots(start.plusDays(2))).extracting(DayLearningTaskView::title).containsExactly("教材2","练习1");
        records.recordDayProgress(request("教材2",start.plusDays(2),30,20));
        assertThat(snapshots(start.plusDays(3))).extracting(DayLearningTaskView::title).containsExactly("教材2","教材3","练习1");
        assertThat(snapshot("教材2",start.plusDays(3)).completionPercent()).isEqualByComparingTo("30");
        // Exercise progress does not require the matching textbook chunk.
        records.recordDayProgress(request("练习1",start,100,15));
        records.recordDayProgress(request("练习2",start.plusDays(1),50,15));
        assertThat(snapshots(start.plusDays(2))).extracting(DayLearningTaskView::title).contains("练习2","练习3");
    }
    @Test void retriesStaleDevicesAndDeleteRecreateAreSafeWithoutInventingQuantity() {
        var stale=request("教材1",start,80,20);
        var req=request("教材1",start,40,30);
        var record=records.recordDayProgress(req);
        assertThat(record.amount()).isNull();
        assertThat(record.continuationDelta()).isEqualByComparingTo("40");
        assertThat(workspace.read(start,start,subjectId).months().getFirst().amounts()).isEmpty();
        assertThat(records.recordDayProgress(req).id()).isEqualTo(record.id());
        assertThat(tasks.progress(ids.get("教材1")).completedAmount()).isEqualByComparingTo("0");
        assertThatThrownBy(()->records.recordDayProgress(stale)).isInstanceOf(BusinessException.class);
        records.delete(record.id(),record.version());
        assertThat(snapshot("教材1",start).completionPercent()).isEqualByComparingTo("0");
        assertThat(snapshots(start.plusDays(1))).extracting(DayLearningTaskView::title).doesNotContain("教材2");
        assertThatThrownBy(()->records.recordDayProgress(req)).isInstanceOf(BusinessException.class);
        records.recordDayProgress(request("教材1",start,40,30));
        assertThat(snapshot("教材1",start).revision()).isNotEqualTo(req.revision());
        var response=today.today(start.plusDays(1));
        assertThat(response.sections().stream().flatMap(s->s.tasks().stream()).filter(t->t.id().equals(ids.get("教材1"))).findFirst().orElseThrow().continuation().title()).isEqualTo("教材");
    }
    @Test void completedTodayStaysVisibleUntilTomorrowAndPercentUsesAllDates() {
        records.recordDayProgress(request("教材1",start,40,10));
        records.recordDayProgress(request("教材1",start.plusDays(1),60,10));
        assertThat(snapshot("教材1",start.plusDays(1)).completionPercent()).isEqualByComparingTo("60");
        records.recordDayProgress(request("教材1",start.plusDays(1),100,10));
        assertThat(snapshot("教材1",start.plusDays(1)).completionPercent()).isEqualByComparingTo("100");
        assertThat(snapshots(start.plusDays(2))).extracting(DayLearningTaskView::title).doesNotContain("教材1");
        assertThatThrownBy(()->records.recordDayProgress(request("教材1",start,80,10))).isInstanceOf(BusinessException.class);
    }
    @Test void strictImportAndReferenceScheduleKeepTheStreamConsistent() {
        for (String bad:List.of(source.replace("\"schemaVersion\":5","\"schemaVersion\":4"),
                source.replace("\"position\":2","\"position\":1"),source.replace("\"position\":2","\"position\":2.5"),
                source.replace("CONTINUED_STUDY","TIMED_STUDY"),source.replace("\"amount\":null","\"amount\":1")))
            assertThatThrownBy(()->imports.preview(bad)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->imports.preview(source)).isInstanceOf(BusinessException.class);
        var ref=daily.list(start).stream().filter(i->i.taskId().equals(ids.get("教材1"))).findFirst().orElseThrow();
        assertThatThrownBy(()->daily.removeItem(start,ref.id(),ref.version())).isInstanceOf(BusinessException.class);
        daily.adjust(start,ref.id(),new DailyItemAdjustRequest(null,ref.version(),"调整预计用时",90));
        assertThat(snapshot("教材1",start).estimatedMinutes()).isEqualTo(90);
        assertThatThrownBy(()->tasks.delete(ids.get("教材1"))).isInstanceOf(BusinessException.class);
    }
    @Test void recordEditingKeepsPercentageAndCannotBypassAdmission() {
        var record=records.recordDayProgress(request("教材1",start,40,30));
        var changed=records.update(record.id(),new StudyRecordUpdateRequest(record.taskId(),null,null,start,null,35,"函数定义",null,record.version()));
        assertThat(changed.continuationDelta()).isEqualByComparingTo("40");
        assertThat(snapshot("教材1",start).completionPercent()).isEqualByComparingTo("40");
        assertThatThrownBy(()->records.update(changed.id(),new StudyRecordUpdateRequest(changed.taskId(),null,null,start.plusDays(1),null,35,null,null,changed.version())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(ids.get("教材3"),null,null,start,null,20,null,null,UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class);
        var ordinary=tasks.create(subjectId,changed.unitId(),"普通用时记录",null);
        var extra=records.create(new StudyRecordCreateRequest(ordinary.getId(),null,null,start,null,10,null,null,UUID.randomUUID().toString()));
        assertThatThrownBy(()->records.update(extra.id(),new StudyRecordUpdateRequest(ids.get("教材3"),null,null,start,null,10,null,null,extra.version())))
                .isInstanceOf(BusinessException.class);
    }
    @Test void sameBookConcurrentChangesRejectOneStaleSnapshot() throws Exception {
        records.recordDayProgress(request("教材1",start,40,20));
        var day=start.plusDays(1);
        var first=request("教材1",day,100,10);var second=request("教材2",day,30,10);
        var startTogether=new java.util.concurrent.CountDownLatch(1);
        try (var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Function<DayProgressRequest,java.util.concurrent.Callable<Boolean>> action=req->()->{
                startTogether.await();
                try { records.recordDayProgress(req); return true; }
                catch (BusinessException stale) { return false; }
            };
            var a=executor.submit(action.apply(first)); var b=executor.submit(action.apply(second));startTogether.countDown();
            assertThat(List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
        }
    }
    @Test void downloadableExampleUsesTwoIndependentStreamsAndVariable120MinuteBudget() throws Exception {
        var sample=java.nio.file.Files.readString(java.nio.file.Path.of("../docs/business/plan-import-continuation-example.json"));
        String subject=subjects.list(true).stream().filter(s->s.getId().equals(subjectId)).findFirst().orElseThrow().getName();
        String unit=units.list(true).getFirst().getName();
        var doc=imports.parse(sample.replace("数学一",subject).replace("\"unit\": \"讲\"","\"unit\": \""+unit+"\"").replace("\"unit\": \"题\"","\"unit\": \""+unit+"\""));
        imports.validate(doc);
        assertThat(doc.tasks()).hasSize(14).allSatisfy(t->assertThat(t.continuation()).isNotNull());
        assertThat(doc.days()).allSatisfy(d->assertThat(d.items().stream().mapToInt(PlanImportDocument.Item::estimatedMinutes).sum()).isEqualTo(120));
    }


    @Test void percentWithoutTimeAdvancesAndDeletionRestoresContent() {
        var dayItem=daily.list(start).stream().filter(p->p.taskId().equals(ids.get("教材1"))).findFirst().orElseThrow();
        daily.adjust(start,dayItem.id(),new DailyItemAdjustRequest(null,dayItem.version(),"无需计时",null));
        assertThat(snapshot("教材1",start).remainingMinutes()).isNull();
        var req=new DayProgressRequest(start,ids.get("教材1"),100,null,snapshot("教材1",start).revision(),UUID.randomUUID().toString());
        var record=records.recordDayProgress(req);
        assertThat(record.durationMinutes()).isNull();
        assertThat(record.continuationDelta()).isEqualByComparingTo("100");
        assertThat(records.recordDayProgress(req).id()).isEqualTo(record.id());
        assertThat(snapshots(start.plusDays(1))).extracting(DayLearningTaskView::title).contains("教材2").doesNotContain("教材1");
        records.delete(record.id(),record.version());
        assertThat(snapshots(start.plusDays(1))).extracting(DayLearningTaskView::title).contains("教材1").doesNotContain("教材2");
    }

}
