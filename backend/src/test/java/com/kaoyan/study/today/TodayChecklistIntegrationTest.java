package com.kaoyan.study.today;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.entity.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import com.kaoyan.study.today.dto.*;
import com.kaoyan.study.today.service.TodayService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
class TodayChecklistIntegrationTest {
 @Autowired StudyRecordService learning;
 @Autowired TaskService tasks;
 @Autowired DailyPlanService plans;
 @Autowired ReviewService reviews;
 @Autowired StudySettingsService settings;
 @Autowired SubjectService subjects;
 @Autowired StudyUnitService units;
 @Autowired TodayService today;
 Task task; LocalDate date;
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
  MySqlTestSupport.registerDataSource(registry);
  registry.add("app.preset-account.username", () -> "kaoyan");
  registry.add("app.preset-account.password", () -> "test-only-password");
 }
 @BeforeEach void setup() {
  String suffix = UUID.randomUUID().toString().substring(0,8);
  var subject = subjects.create(new SubjectRequest("今日测试-"+suffix, null, null, 0, true));
  var unit = units.create(new StudyUnitRequest("今日单位-"+suffix, 0, true));
  task = tasks.create(new TaskCreateRequest(subject.getId(), null, "今日学习", unit.getId(), BigDecimal.TEN));
  date = LocalDate.of(2040,1,1).plusDays(ThreadLocalRandom.current().nextInt(100000));
  plans.addItem(date, new DailyItemCreateRequest(task.getId(), BigDecimal.TEN, null, 100));
 }
 DayLearningTaskView snapshot() { return learning.dayTasks(date).stream().filter(x -> x.id().equals(task.getId())).findFirst().orElseThrow(); }
 StudyRecordView save(int percent, int minutes) { return learning.recordDayProgress(new DayProgressRequest(date, task.getId(), percent, minutes, snapshot().revision(), UUID.randomUUID().toString())); }
 TodaySubjectSection section() { return today.today(date).sections().stream().filter(x -> x.name().startsWith("今日测试-")).findFirst().orElseThrow(); }
 @Test void cumulativePercentCreatesOnlyDeltaAndRetainsTime() {
  assertThat(save(30,25).amount()).isEqualByComparingTo("3");
  assertThat(save(50,15).amount()).isEqualByComparingTo("2");
  assertThat(snapshot().remainingMinutes()).isEqualByComparingTo("50");
  assertThat(save(50,5).amount()).isNull();
  assertThat(save(100,40).amount()).isEqualByComparingTo("5");
  assertThat(tasks.progress(task.getId()).completedAmount()).isEqualByComparingTo("10");
  assertThat(section().actualMinutes()).isEqualTo(85);
  assertThat(section().remainingMinutes()).isEqualByComparingTo("0");
  assertThat(today.today(date).actualMinutes()).isEqualTo(85);
 }
 @Test void retryIsIdempotentWithOldRevision() {
  var req = new DayProgressRequest(date, task.getId(),30,20,snapshot().revision(),UUID.randomUUID().toString());
  var one=learning.recordDayProgress(req);
  assertThat(learning.recordDayProgress(req).id()).isEqualTo(one.id());
  assertThat(section().actualMinutes()).isEqualTo(20);
 }
 @Test void recordAndPlanEditsRejectStaleSnapshotsAndDeletionRecalculates() {
  var old=snapshot(); var record=save(30,20);
  assertThatThrownBy(() -> learning.recordDayProgress(new DayProgressRequest(date,task.getId(),50,10,old.revision(),UUID.randomUUID().toString()))).isInstanceOf(ConflictException.class);
  var beforeEdit=snapshot();
  learning.update(record.id(),new StudyRecordUpdateRequest(task.getId(),null,null,date,new BigDecimal("4"),35,null,null,record.version()));
  assertThat(snapshot().completionPercent()).isEqualByComparingTo("40");
  assertThat(section().actualMinutes()).isEqualTo(35);
  assertThatThrownBy(() -> learning.recordDayProgress(new DayProgressRequest(date,task.getId(),50,10,beforeEdit.revision(),UUID.randomUUID().toString()))).isInstanceOf(ConflictException.class);
  var beforePlan=snapshot();var plan=plans.list(date).getFirst();
  plans.adjust(date,plan.id(),new DailyItemAdjustRequest(new BigDecimal("20"),plan.version(),"增加",200));
  assertThat(snapshot().remainingMinutes()).isEqualByComparingTo("160");
  assertThatThrownBy(() -> learning.recordDayProgress(new DayProgressRequest(date,task.getId(),50,10,beforePlan.revision(),UUID.randomUUID().toString()))).isInstanceOf(ConflictException.class);
  learning.delete(record.id(),learning.get(record.id()).version());
  assertThat(snapshot().completionPercent()).isEqualByComparingTo("0");
  assertThat(section().actualMinutes()).isZero();
 }
 @Test void simultaneousSubmissionsDoNotDoubleCompletion() throws Exception {
  var revision=snapshot().revision();var gate=new CyclicBarrier(2);
  try(var executor=Executors.newFixedThreadPool(2)) {
   Callable<String> action=()-> {gate.await(5,TimeUnit.SECONDS);try{learning.recordDayProgress(new DayProgressRequest(date,task.getId(),30,20,revision,UUID.randomUUID().toString()));return "saved";}catch(ConflictException e){return "stale";}};
   var a=executor.submit(action);var b=executor.submit(action);
   assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder("saved","stale");
  }
  assertThat(snapshot().completedAmount()).isEqualByComparingTo("3");
  assertThat(section().actualMinutes()).isEqualTo(20);
 }
 @Test void multipleSourcesShareChecklistAndMissingEstimateIsExplicit() {
  plans.addItem(date,new DailyItemCreateRequest(task.getId(),BigDecimal.TEN,PlanSource.WEEKLY,null));
  assertThat(section().tasks()).hasSize(1);
  assertThat(snapshot().plannedAmount()).isEqualByComparingTo("20");
  assertThat(section().missingEstimates()).isEqualTo(1);
  save(50,25);assertThat(snapshot().completedAmount()).isEqualByComparingTo("10");
  assertThat(section().actualMinutes()).isEqualTo(25);
 }
 @Test void fractionalQuantityIsNotRoundedAway() {
  var plan=plans.list(date).getFirst();plans.adjust(date,plan.id(),new DailyItemAdjustRequest(new BigDecimal("0.01"),plan.version(),null,10));
  assertThat(save(30,5).amount()).isEqualByComparingTo("0.003");
  assertThat(save(50,5).amount()).isEqualByComparingTo("0.002");
  assertThat(snapshot().completionPercent()).isEqualByComparingTo("50");
 }
 @Test void reviewPartialAndCompleteRetainHistoryWithoutIncreasingFirstLearning() {
  learning.create(new StudyRecordCreateRequest(task.getId(),null,null,LocalDate.now().minusDays(1),BigDecimal.ONE,10,null,null,null));
  var item=reviews.addToReview(new ReviewItemCreateRequest(task.getId(),null,null,40,null));
  var config=settings.get();settings.update(new StudySettingsRequest(null,120,60,ReviewPhase.MAIN,1,3,7,config.getVersion()));
  reviews.confirm(new ReviewConfirmRequest(date,List.of(new ReviewConfirmRequest.Item(item.getId(),40))));
  var req=new ReviewDayProgressRequest(item.getId(),date,30,12,reviews.get(item.getId()).getVersion(),null,UUID.randomUUID().toString());
  var partial=reviews.recordDayProgress(req);
  assertThat(reviews.recordDayProgress(req).id()).isEqualTo(partial.id());
  assertThat(reviews.get(item.getId()).getStatus()).isEqualTo(ReviewStatus.SCHEDULED);
  assertThat(reviews.get(item.getId()).getLastResult()).isNull();
  assertThatThrownBy(()->reviews.recordDayProgress(new ReviewDayProgressRequest(item.getId(),date,50,8,req.version(),null,UUID.randomUUID().toString()))).isInstanceOf(ConflictException.class);
  reviews.recordDayProgress(new ReviewDayProgressRequest(item.getId(),date,50,8,reviews.get(item.getId()).getVersion(),null,UUID.randomUUID().toString()));
  reviews.recordDayProgress(new ReviewDayProgressRequest(item.getId(),date,100,20,reviews.get(item.getId()).getVersion(),MasteryResult.MASTERED,UUID.randomUUID().toString()));
  assertThat(reviews.get(item.getId()).getScheduledDate()).isNull();
  assertThat(reviews.get(item.getId()).getNextReviewDate()).isEqualTo(date.plusDays(7));
  var day=reviews.dayTasks(date).stream().filter(x->x.id().equals(item.getId())).findFirst().orElseThrow();
  assertThat(day.completionPercent()).isEqualByComparingTo("100");assertThat(day.actualMinutes()).isEqualTo(40);
  assertThat(section().actualMinutes()).isEqualTo(40);assertThat(section().tasks()).hasSize(2);
  assertThat(tasks.progress(task.getId()).completedAmount()).isEqualByComparingTo("1");
  assertThat(today.today(date).actualMinutes()).isEqualTo(today.today(date).sections().stream().mapToInt(TodaySubjectSection::actualMinutes).sum());
 }
}
