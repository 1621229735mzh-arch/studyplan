package com.kaoyan.study.review;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.PlanImportRequest;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.dto.AutoReviewContracts.*;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.mapper.*;
import com.kaoyan.study.review.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"app.preset-account.username=fsrs-test","app.preset-account.password=test-only-password",
        "app.review.automatic-reconciliation-enabled=false"})
@AutoConfigureMockMvc
@Transactional
class AutoReviewIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) { MySqlTestSupport.registerDataSource(r); }
    @Autowired AutoReviewService automatic;
    @Autowired AutoReviewMapper auto;
    @Autowired ReviewService reviews;
    @Autowired ReviewMapper mapper;
    @Autowired ReviewWorkspaceService workspace;
    @Autowired StudyRecordService records;
    @Autowired PlanImportService imports;
    @Autowired TaskService tasks;
    @Autowired TaskContinuationService continuations;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired StudyDay studyDay;
    @Autowired MockMvc mvc;
    @Autowired com.kaoyan.study.today.service.TodayService todayService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    Long subjectId,first,second,other,stream;
    LocalDate start;

    @BeforeEach void setup() {
        var suffix=UUID.randomUUID().toString().substring(0,8);
        String subject="自动复习-"+suffix,unit="段-"+suffix;
        subjectId=subjects.create(new SubjectRequest(subject,"数学",null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        start=studyDay.today().minusDays(20);
        var dates=List.of(start,start.plusDays(1));
        String months=String.join(",",dates.stream().map(d->d.toString().substring(0,7)).distinct()
                .map(m->"{\"month\":\""+m+"\",\"title\":\"参考学习\"}").toList());
        String weeks=String.join(",",dates.stream().map(d->d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))).distinct()
                .map(d->"{\"startDate\":\""+d+"\",\"title\":\"参考学习\"}").toList());
        var content=new ArrayList<String>();
        for(int i=1;i<=3;i++) content.add("""
            {"key":"t%d","subject":"%s","unit":"%s","title":"内容%d",
             "activity":{"mode":"CONTINUED_STUDY","description":"具体内容%d"},
             "continuation":{"streamKey":"%s","title":"%s","position":%d}}
            """.formatted(i,subject,unit,i,i,i==3?"practice":"textbook",i==3?"1000题":"基础30讲",i==3?1:i));
        String source="""
            {"schemaVersion":5,"planId":"auto-%s","title":"自动复习测试","startDate":"%s","endDate":"%s",
             "goals":[],"months":[%s],"weeks":[%s],"tasks":[%s],
             "days":[{"date":"%s","items":[{"taskKey":"t1","amount":null},{"taskKey":"t3","amount":null}]},
                     {"date":"%s","items":[{"taskKey":"t2","amount":null}]}]}
            """.formatted(suffix,start,start.plusDays(1),months,weeks,String.join(",",content),start,start.plusDays(1));
        imports.confirm(new PlanImportRequest(source,imports.preview(source).previewToken()));
        var all=tasks.list(subjectId,null,null);
        first=all.stream().filter(t->t.getTitle().equals("内容1")).findFirst().orElseThrow().getId();
        second=all.stream().filter(t->t.getTitle().equals("内容2")).findFirst().orElseThrow().getId();
        other=all.stream().filter(t->t.getTitle().equals("内容3")).findFirst().orElseThrow().getId();
        stream=continuations.streamId(first);
    }
    Book book() { return automatic.books().stream().filter(b->b.streamId().equals(stream)).findFirst().orElseThrow(); }
    void enable() { automatic.setPolicy(stream,new PolicyRequest(true,book().version())); }
    DayProgressRequest learning(Long id,LocalDate date,int percent) {
        var snapshot=records.dayTasks(date).stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
        return new DayProgressRequest(date,id,percent,null,snapshot.revision(),UUID.randomUUID().toString());
    }
    ReviewItem item() { return mapper.findItemByTaskId(first); }
    ReviewRecordCreateRequest feedback(LocalDate date,MasteryResult result) {
        return new ReviewRecordCreateRequest(item().getId(),date,result,null,null,null,UUID.randomUUID().toString(),item().getVersion());
    }

    @Test void optInRequiresFullSegmentAndUsesActualCompletionDateWithIndependentBooks() {
        enable(); records.recordDayProgress(learning(first,start,40));
        assertThat(item()).isNull();
        records.recordDayProgress(learning(other,start,100));
        assertThat(mapper.findItemByTaskId(other)).isNull();
        records.recordDayProgress(learning(first,start.plusDays(3),100));
        assertThat(item().getScheduledDate()).isEqualTo(start.plusDays(4));
        assertThat(auto.state(item().getId()).completedOn()).isEqualTo(start.plusDays(3));
        assertThat(auto.state(item().getId()).stability()).isNull();
        assertThat(auto.state(item().getId()).repetitions()).isZero();
        assertThat(reviews.dayTasks(start.plusDays(4))).anyMatch(r->r.id().equals(item().getId()) && r.title().contains("基础30讲"));
        assertThat(workspace.workspace(subjectId).entries()).singleElement().satisfies(e->{
            assertThat(e.bookTitle()).isEqualTo("基础30讲"); assertThat(e.round()).isEqualTo(1);
        });
    }
    @Test void backfillKeepsOverdueDatesAndEnablingTwiceDoesNotCreateFacts() {
        records.recordDayProgress(learning(first,start,100)); assertThat(item()).isNull();
        enable(); Long id=item().getId();
        automatic.setPolicy(stream,new PolicyRequest(true,book().version())); automatic.reconcile(stream);
        assertThat(item().getId()).isEqualTo(id); assertThat(item().getScheduledDate()).isEqualTo(start.plusDays(1));
        assertThat(book().overdueCount()).isEqualTo(1); assertThat(reviews.records(id)).isEmpty();
        assertThat(records.list(null,null,first,null)).hasSize(1);
    }
    @Test void feedbackCreatesNextScheduledRoundFromRealReviewDateAndRetryDoesNotAdvanceMemory() {
        enable(); records.recordDayProgress(learning(first,start,100));
        LocalDate actual=start.plusDays(5); var request=feedback(actual,MasteryResult.MASTERED);
        var response=reviews.recordReview(request);
        assertThat(response.nextReviewDate()).isEqualTo(actual.plusDays(3));
        assertThat(item().getStatus()).isEqualTo(ReviewStatus.SCHEDULED);
        assertThat(item().getScheduledDate()).isEqualTo(actual.plusDays(3));
        assertThat(auto.state(item().getId()).repetitions()).isEqualTo(1);
        assertThat(reviews.recordReview(request).id()).isEqualTo(response.id());
        assertThat(auto.state(item().getId()).repetitions()).isEqualTo(1);
        assertThatThrownBy(()->reviews.recordReview(new ReviewRecordCreateRequest(item().getId(),actual,MasteryResult.FORGOT,null,null,null,request.clientToken())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("令牌");
        assertThat(workspace.workspace(subjectId).entries()).filteredOn(e->e.date().equals(actual.plusDays(3)))
                .singleElement().satisfies(e->assertThat(e.round()).isEqualTo(2));
        assertThat(records.list(null,null,first,null)).hasSize(1);
    }
    @Test void partialProgressDoesNotUpdateMemoryAndFullFeedbackHandlesDayRouteReplay() {
        enable(); records.recordDayProgress(learning(first,start,100));
        var date=start.plusDays(1); var id=item().getId();
        var partial=new ReviewDayProgressRequest(id,date,40,null,item().getVersion(),null,UUID.randomUUID().toString());
        reviews.recordDayProgress(partial); assertThat(auto.state(id).repetitions()).isZero();
        assertThat(item().getScheduledDate()).isEqualTo(date);
        var full=new ReviewDayProgressRequest(id,date,100,null,item().getVersion(),MasteryResult.FORGOT,UUID.randomUUID().toString());
        var done=reviews.recordDayProgress(full);
        assertThat(done.progressDelta()).isEqualByComparingTo("60");
        assertThat(item().getScheduledDate()).isEqualTo(date.plusDays(1));
        assertThat(reviews.recordDayProgress(full).id()).isEqualTo(done.id());
        assertThat(auto.state(id).repetitions()).isEqualTo(1);
    }
    @Test void deletingSourceSuspendsNextDateAndKeepsReviewHistoryThenRecompletionRestoresIdentity() {
        enable(); var record=records.recordDayProgress(learning(first,start,100));
        reviews.recordReview(feedback(start.plusDays(1),MasteryResult.MASTERED));
        Long id=item().getId(); records.delete(record.id(),record.version());
        assertThat(item().getScheduledDate()).isNull(); assertThat(auto.state(id).sourceValid()).isFalse();
        assertThat(reviews.records(id)).hasSize(1);
        assertThatThrownBy(()->workspace.schedule(new ReviewScheduleRequest(id,studyDay.today(),item().getVersion())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("来源");
        records.recordDayProgress(learning(first,start.plusDays(8),100));
        assertThat(item().getId()).isEqualTo(id); assertThat(auto.state(id).sourceValid()).isTrue();
        assertThat(item().getScheduledDate()).isEqualTo(start.plusDays(9));
        assertThat(reviews.records(id)).hasSize(1);
    }
    @Test void pausedBooksAndRemovedIndividualDatesStayPausedAcrossReconciliation() {
        enable(); records.recordDayProgress(learning(first,start,100));
        var id=item().getId();
        workspace.schedule(new ReviewScheduleRequest(id,null,item().getVersion())); automatic.reconcile(stream);
        assertThat(item().getScheduledDate()).isNull(); assertThat(auto.state(id).paused()).isTrue();
        workspace.schedule(new ReviewScheduleRequest(id,studyDay.today(),item().getVersion()));
        automatic.setPolicy(stream,new PolicyRequest(false,book().version())); automatic.reconcile(stream);
        assertThat(item().getScheduledDate()).isNull(); assertThat(reviews.records(id)).isEmpty();
        enable(); assertThat(item().getScheduledDate()).isEqualTo(studyDay.today());
    }
    @Test void manualAndArchivedItemsAreNotConvertedOrRevived() {
        records.recordDayProgress(learning(first,start,100));
        reviews.addToReview(new ReviewItemCreateRequest(first,null,null,null));
        var manual=item(); enable();
        assertThat(auto.state(manual.getId())).isNull(); assertThat(item().getVersion()).isEqualTo(manual.getVersion());
        records.recordDayProgress(learning(second,start.plusDays(1),100));
        var managed=mapper.findItemByTaskId(second);
        reviews.archive(managed.getId(),managed.getVersion()); automatic.reconcile(stream);
        assertThat(reviews.get(managed.getId()).getStatus()).isEqualTo(ReviewStatus.ARCHIVED);
    }
    @Test void futureLearningDoesNotBecomeEligibleBeforeCutoffAndReconciliationUsesStudyDay() {
        enable(); records.recordDayProgress(learning(first,studyDay.today().plusDays(1),100));
        automatic.reconcile(stream); assertThat(item()).isNull();
    }
    @Test void stalePoliciesAndDatesAndDuplicateDayFeedbackAreRejected() {
        enable(); records.recordDayProgress(learning(first,start,100));
        assertThatThrownBy(()->automatic.setPolicy(stream,new PolicyRequest(false,0L))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->reviews.recordReview(feedback(start,MasteryResult.MASTERED))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->reviews.recordReview(feedback(studyDay.today().plusDays(1),MasteryResult.MASTERED))).isInstanceOf(BusinessException.class);
        var actual=start.plusDays(1); reviews.recordReview(feedback(actual,MasteryResult.MASTERED));
        assertThatThrownBy(()->reviews.recordReview(feedback(actual,MasteryResult.FORGOT))).isInstanceOf(BusinessException.class);
        assertThat(auto.state(item().getId()).repetitions()).isEqualTo(1);
    }
    @Test void staleFeedbackCannotOverwriteManualReschedulingAndArchivedFeedbackCannotAdvance() {
        enable(); records.recordDayProgress(learning(first,start,100));
        var stale=feedback(start.plusDays(1),MasteryResult.MASTERED);
        workspace.schedule(new ReviewScheduleRequest(item().getId(),studyDay.today(),item().getVersion()));
        assertThatThrownBy(()->reviews.recordReview(stale)).isInstanceOf(BusinessException.class).hasMessageContaining("已变化");
        assertThat(auto.state(item().getId()).repetitions()).isZero();
        var current=feedback(studyDay.today(),MasteryResult.MASTERED);
        reviews.archive(item().getId(),item().getVersion());
        assertThatThrownBy(()->reviews.recordReview(current)).isInstanceOf(BusinessException.class).hasMessageContaining("归档");
        assertThat(auto.state(item().getId()).repetitions()).isZero();
    }
    @Test void endpointsRequireAuthenticationCsrfAndValidPolicy() throws Exception {
        mvc.perform(get("/api/review/automatic/books")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/review/automatic/books/"+stream).with(user("fsrs-test"))
                .contentType("application/json").content("{\"enabled\":true,\"version\":0}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/review/automatic/books/"+stream).with(user("fsrs-test")).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"version\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/review/automatic/books/"+stream).with(user("fsrs-test")).with(csrf())
                .contentType("application/json").content("{\"enabled\":true,\"version\":0}")).andExpect(status().isOk());
    }
    @Test void todaySelectsOverdueFirstWithoutMovingDatesAndCompletionOffersOptionalNext() {
        enable(); records.recordDayProgress(learning(first,start,100));
        records.recordDayProgress(learning(second,start.plusDays(1),100));
        var next=mapper.findItemByTaskId(second);var day=studyDay.today();
        workspace.schedule(new ReviewScheduleRequest(next.getId(),day,next.getVersion()));
        var original=item();
        var queue=reviews.todayTasks(day).stream().filter(t->t.automaticReview()!=null && t.automaticReview().streamId().equals(stream)).toList();
        assertThat(queue).singleElement().satisfies(q->{
            assertThat(q.task().id()).isEqualTo(original.getId());
            assertThat(q.automaticReview().dueDate()).isEqualTo(start.plusDays(1));
            assertThat(q.automaticReview().optional()).isFalse();
            assertThat(q.automaticReview().remainingCount()).isEqualTo(2);
        });
        assertThat(item().getVersion()).isEqualTo(original.getVersion());
        assertThat(item().getScheduledDate()).isEqualTo(original.getScheduledDate());
        var partial=new ReviewDayProgressRequest(original.getId(),day,40,null,item().getVersion(),null,UUID.randomUUID().toString());
        var saved=reviews.recordDayProgress(partial);
        assertThat(reviews.recordDayProgress(partial).id()).isEqualTo(saved.id());
        assertThat(item().getScheduledDate()).isEqualTo(start.plusDays(1));
        assertThat(auto.state(original.getId()).repetitions()).isZero();
        var done=reviews.recordDayProgress(new ReviewDayProgressRequest(original.getId(),day,100,null,item().getVersion(),MasteryResult.MASTERED,UUID.randomUUID().toString()));
        assertThat(done.progressDelta()).isEqualByComparingTo("60");
        assertThat(item().getScheduledDate()).isEqualTo(day.plusDays(3));
        var updated=reviews.todayTasks(day).stream().filter(t->t.automaticReview()!=null && t.automaticReview().streamId().equals(stream)).toList();
        assertThat(updated).hasSize(2);
        assertThat(updated.stream().filter(q->q.task().id().equals(next.getId()))).singleElement().satisfies(q->assertThat(q.automaticReview().optional()).isTrue());
        var section=todayService.today(day).sections().stream().filter(s->s.tasks().stream().anyMatch(t->original.getId().equals(t.id()) && "REVIEW".equals(t.kind()))).findFirst().orElseThrow();
        assertThat(section.tasks().stream().filter(t->t.automaticReview()!=null && t.completionPercent().intValue()==100)).hasSize(1);
        assertThat(section.tasks().stream().filter(t->t.automaticReview()!=null && t.automaticReview().optional())).hasSize(1);
        var nextCurrent=mapper.findItemByTaskId(second);
        reviews.recordDayProgress(new ReviewDayProgressRequest(nextCurrent.getId(),day,25,null,nextCurrent.getVersion(),null,UUID.randomUUID().toString()));
        assertThat(reviews.todayTasks(day).stream().filter(q->q.task().id().equals(mapper.findItemByTaskId(second).getId())))
                .singleElement().satisfies(q->assertThat(q.automaticReview().optional()).isFalse());
        assertThat(records.list(null,null,first,null)).hasSize(1);
        assertThat(records.list(null,null,second,null)).hasSize(1);
    }
    @Test void queuePinsTodaysPartialBeforeOtherOverdueAndExcludesFuturePausedAndInvalidSources() {
        enable(); var original=records.recordDayProgress(learning(first,start,100));
        records.recordDayProgress(learning(second,start.plusDays(1),100));
        var day=studyDay.today();
        workspace.schedule(new ReviewScheduleRequest(item().getId(),day,item().getVersion()));
        reviews.recordDayProgress(new ReviewDayProgressRequest(item().getId(),day,30,null,item().getVersion(),null,UUID.randomUUID().toString()));
        assertThat(automatic.dayQueue(day).stream().filter(q->q.automaticReview().streamId().equals(stream)))
                .singleElement().satisfies(q->assertThat(q.task().id()).isEqualTo(item().getId()));
        assertThat(automatic.dayQueue(day.plusDays(1))).isEmpty();
        var next=mapper.findItemByTaskId(second);
        workspace.schedule(new ReviewScheduleRequest(next.getId(),null,next.getVersion()));
        records.delete(original.id(),original.version());
        assertThat(automatic.dayQueue(day).stream().filter(q->q.automaticReview().streamId().equals(stream))).isEmpty();
        assertThatThrownBy(()->reviews.recordDayProgress(new ReviewDayProgressRequest(item().getId(),day,100,null,item().getVersion(),MasteryResult.MASTERED,UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class);
        assertThat(reviews.records(item().getId())).hasSize(1);
    }
    @Test void overduePriorityUsesRelativeLatenessAndManualItemsKeepExactDates() {
        enable(); records.recordDayProgress(learning(first,start,100));
        records.recordDayProgress(learning(second,start.plusDays(1),100));
        jdbc.update("UPDATE review_item SET interval_days=100 WHERE id=?",item().getId());
        assertThat(automatic.dayQueue(studyDay.today()).stream().filter(q->q.automaticReview().streamId().equals(stream)))
                .singleElement().satisfies(q->assertThat(q.task().id()).isEqualTo(mapper.findItemByTaskId(second).getId()));
        assertThat(reviews.todayTasks(studyDay.today().minusDays(1)).stream().filter(q->q.task().id().equals(item().getId()))).isEmpty();
        records.recordDayProgress(learning(other,start,100));
        var manual=reviews.addToReview(new ReviewItemCreateRequest(other,null,null,null));
        workspace.schedule(new ReviewScheduleRequest(manual.getId(),start.plusDays(1),manual.getVersion()));
        manual=reviews.get(manual.getId());
        Long manualId=manual.getId();
        assertThat(reviews.todayTasks(studyDay.today()).stream().filter(q->q.task().id().equals(manualId))).isEmpty();
        var request=new ReviewDayProgressRequest(manual.getId(),studyDay.today(),100,null,manual.getVersion(),MasteryResult.MASTERED,UUID.randomUUID().toString());
        assertThatThrownBy(()->reviews.recordDayProgress(request)).isInstanceOf(BusinessException.class).hasMessageContaining("未安排");
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentEnablingAndCompletionCannotDuplicateTheReviewItem() throws Exception {
        var req=learning(first,start,100); var latch=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{latch.await(); automatic.setPolicy(stream,new PolicyRequest(true,0L)); return true;});
            var b=pool.submit(()->{latch.await(); records.recordDayProgress(req); return true;});
            latch.countDown(); assertThat(a.get(20,TimeUnit.SECONDS)).isTrue(); assertThat(b.get(20,TimeUnit.SECONDS)).isTrue();
        }
        assertThat(automatic.segments().stream().filter(s->s.streamId().equals(stream))).hasSize(1);
        assertThat(records.list(null,null,first,null)).hasSize(1);
    }
}
