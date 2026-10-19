package com.kaoyan.study.learning;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.*;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SequenceLearningIntegrationTest {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired com.kaoyan.study.today.service.TodayService today;
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired SequenceLearningService sequences;
    @Autowired TaskService tasks;
    @Autowired ProblemNumberModeService numberMode;
    @Autowired DailyPlanService daily;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired JdbcTemplate jdbc;
    LocalDate date=LocalDate.of(2026,10,14);
    String source;
    Long taskId,subjectId;
    List<SequenceLearningService.Entry> items;
    @DynamicPropertySource static void config(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username",()->"kaoyan");
        registry.add("app.preset-account.password",()->"test-only-password");
    }
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        String subject="接续-"+suffix,unit="题-"+suffix;
        subjectId=subjects.create(new SubjectRequest(subject,null,null,0,true)).getId();
        units.create(new StudyUnitRequest(unit,0,true));
        source="""
          {"schemaVersion":2,"planId":"%s","title":"接续测试","startDate":"2026-10-14","endDate":"2026-10-16",
           "goals":[],"months":[{"month":"2026-10","title":"本月"}],"weeks":[{"startDate":"2026-10-12","title":"本周"}],
           "tasks":[{"key":"leetcode","subject":"%s","unit":"%s","title":"每日一题","plannedAmount":3,
             "sequence":{"mode":"COMPLETE_THEN_ADVANCE","items":[
               {"key":"31","title":"31 Next Permutation","url":"https://leetcode.cn/problems/next-permutation/"},
               {"key":"32","title":"32 Longest Valid Parentheses","url":"https://leetcode.cn/problems/longest-valid-parentheses/"},
               {"key":"33","title":"33 Search in Rotated Sorted Array","url":"https://leetcode.cn/problems/search-in-rotated-sorted-array/"}]}}],
           "days":[{"date":"2026-10-14","items":[{"taskKey":"leetcode","amount":1,"estimatedMinutes":null}]},
                   {"date":"2026-10-15","items":[{"taskKey":"leetcode","amount":1,"estimatedMinutes":null}]},
                   {"date":"2026-10-16","items":[{"taskKey":"leetcode","amount":1,"estimatedMinutes":null}]}]}
          """.formatted(UUID.randomUUID(),subject,unit);
        var preview=imports.preview(source);
        assertThat(tasks.list(subjectId,null,null)).isEmpty();
        imports.confirm(new PlanImportRequest(source,preview.previewToken()));
        taskId=tasks.list(subjectId,null,null).getFirst().getId();
        items=sequences.state(taskId,date).items();
    }
    StudyRecordCreateRequest request(int day,int item,BigDecimal amount,String token) {
        return new StudyRecordCreateRequest(taskId,null,null,date.plusDays(day),amount,20,null,null,token,items.get(item).id());
    }
    StudyRecordView complete(int day,int item) { return records.create(request(day,item,BigDecimal.ONE,UUID.randomUUID().toString())); }
    @Test void attemptsAndMissedDaysDoNotAdvanceOrAccumulate() {
        var attempt=records.create(request(0,0,null,UUID.randomUUID().toString()));
        assertThat(attempt.sequenceItemId()).isEqualTo(items.getFirst().id());
        assertThat(sequences.state(taskId,date.plusDays(1)).dayItem().key()).isEqualTo("31");
        assertThat(records.dayTasks(date.plusDays(1)).stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow().plannedAmount()).isEqualByComparingTo("1");
        complete(1,0);
        assertThat(sequences.state(taskId,date.plusDays(1)).dayItem().key()).isEqualTo("31");
        assertThat(sequences.state(taskId,date.plusDays(2)).current().key()).isEqualTo("32");
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("1");
    }
    @Test void rejectsFractionsSecondDailyCompletionWrongItemAndRange() {
        assertThatThrownBy(()->records.create(request(0,0,new BigDecimal("0.5"),null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->complete(0,1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(request(-1,0,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(request(3,0,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        complete(0,0);
        assertThatThrownBy(()->complete(0,1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->complete(1,0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(taskId,null,null,date.plusDays(1),BigDecimal.ONE,20,null,null,null))).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).hasSize(1);
    }
    @Test void deletingEarlierCompletionRestoresGapAndKeepsLaterIdentity() {
        var first=complete(0,0); var second=complete(1,1);
        records.delete(first.id(),first.version());
        assertThat(sequences.state(taskId,date.plusDays(2)).current().key()).isEqualTo("31");
        assertThat(records.get(second.id()).sequenceItemId()).isEqualTo(items.get(1).id());
        complete(2,0);
        assertThat(sequences.state(taskId,date.plusDays(2)).current().key()).isEqualTo("33");
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("2");
    }
    @Test void editingCompletionToAttemptRecalculatesAndKeepsIdentity() {
        var first=complete(0,0); complete(1,1);
        var changed=records.update(first.id(),new StudyRecordUpdateRequest(taskId,null,null,date,null,15,"没做完",null,first.version()));
        assertThat(changed.sequenceItemId()).isEqualTo(first.sequenceItemId());
        assertThat(sequences.state(taskId,date.plusDays(2)).current().key()).isEqualTo("31");
        assertThatThrownBy(()->records.update(first.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.ONE,20,null,null,first.version()))).isInstanceOf(BusinessException.class);
        records.update(first.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.ONE,25,null,null,changed.version()));
        assertThat(sequences.state(taskId,date.plusDays(2)).current().key()).isEqualTo("33");
    }
    @Test void dayProgressChecksCrossDayRevisionAndDoesNotCountPartialAttempts() {
        var snapshot=records.dayTasks(date.plusDays(1)).stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow();
        complete(0,0);
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date.plusDays(1),taskId,100,20,snapshot.revision(),UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        var refreshed=records.dayTasks(date.plusDays(1)).stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow();
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date.plusDays(1),taskId,50,20,refreshed.revision(),UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        var attempt=records.recordDayProgress(new DayProgressRequest(date.plusDays(1),taskId,0,20,refreshed.revision(),UUID.randomUUID().toString()));
        assertThat(attempt.amount()).isNull();
        assertThat(attempt.sequenceItemId()).isEqualTo(items.get(1).id());
    }
    @Test void concurrentDuplicateRetriesReturnSameRecordAndDifferentTokensCannotDoubleComplete() throws Exception {
        var req=request(0,0,BigDecimal.ONE,UUID.randomUUID().toString());
        var gate=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Long> write=()-> { gate.await(); return records.create(req).id(); };
            var a=pool.submit(write); var b=pool.submit(write); gate.countDown();
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }
        assertThat(records.list(null,null,taskId,null)).hasSize(1);
        var gate2=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> write=()-> { gate2.await(); try { complete(1,1); return true; } catch (BusinessException e) { return false; } };
            var a=pool.submit(write); var b=pool.submit(write); gate2.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(records.list(null,null,taskId,null)).hasSize(2);
    }
    @Test void sequencePlanCannotGainExtraSlotsOrChangeUnitTotal() {
        assertThatThrownBy(()->daily.addItem(date,new DailyItemCreateRequest(taskId,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        var item=daily.list(date).stream().filter(i -> i.taskId().equals(taskId)).findFirst().orElseThrow();
        assertThatThrownBy(()->daily.adjust(date,item.id(),new DailyItemAdjustRequest(BigDecimal.TEN,item.version(),"多做"))).isInstanceOf(BusinessException.class);
        var task=tasks.get(taskId);
        assertThatThrownBy(()->tasks.update(taskId,new TaskUpdateRequest(task.getSubjectId(),null,task.getTitle(),task.getUnitId(),BigDecimal.TEN,task.getStatus(),task.getVersion()))).isInstanceOf(BusinessException.class);
    }
    @Test void strictImportRejectsWrongVersionModeLinkNullAndDuplicateItems() {
        for (String invalid:List.of(source.replace("\"schemaVersion\":2","\"schemaVersion\":1"),
                source.replace("COMPLETE_THEN_ADVANCE","BY_DATE"),source.replace("https://leetcode.cn/problems/next-permutation/","javascript:alert(1)"),
                source.replace("\"key\":\"32\"","\"key\":\"31\""),source.replace("\"plannedAmount\":3","\"plannedAmount\":4")))
            assertThatThrownBy(()->imports.preview(invalid)).isInstanceOf(BusinessException.class);
    }

    @Test void untimedAttemptDoesNotAdvanceAndUntimedCompletionDoes() {
        var snapshot=records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        var attempt=new DayProgressRequest(date,taskId,0,null,snapshot.revision(),UUID.randomUUID().toString());
        var saved=records.recordDayProgress(attempt);
        assertThat(saved.durationMinutes()).isNull();
        assertThat(saved.amount()).isNull();
        assertThat(records.recordDayProgress(attempt).id()).isEqualTo(saved.id());
        assertThat(sequences.state(taskId,date).current().key()).isEqualTo("31");
        snapshot=records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        records.recordDayProgress(new DayProgressRequest(date,taskId,100,null,snapshot.revision(),UUID.randomUUID().toString()));
        assertThat(sequences.state(taskId,date.plusDays(1)).current().key()).isEqualTo("32");
    }

    void convertNumbers(List<Integer> numbers) {
        var preview=numberMode.preview(taskId);
        var corrections=new ArrayList<ProblemNumberModeService.Correction>();
        for(int i=0;i<preview.history().size();i++) {
            var r=preview.history().get(i).record();
            corrections.add(new ProblemNumberModeService.Correction(r.id(),r.version(),numbers.get(i)));
        }
        numberMode.confirm(taskId,new ProblemNumberModeService.Confirm("LeetCode每日一题：自行填写题号",preview.version(),preview.workspaceRevision(),preview.revision(),corrections));
    }
    StudyRecordCreateRequest numbered(int day,int number,BigDecimal amount,String token) {
        return new StudyRecordCreateRequest(taskId,null,null,date.plusDays(day),amount,null,null,null,token,null,number);
    }
    @Test void convertsExistingThreeRecordsTo31_32_49InPlace() {
        var first=complete(0,0);var second=complete(1,1);var third=complete(2,2);
        convertNumbers(List.of(31,32,49));
        assertThat(records.get(first.id())).usingRecursiveComparison().ignoringFields("taskTitle").isEqualTo(first);
        assertThat(records.get(second.id())).usingRecursiveComparison().ignoringFields("taskTitle").isEqualTo(second);
        var corrected=records.get(third.id());
        assertThat(corrected.recordDate()).isEqualTo(third.recordDate());
        assertThat(corrected.durationMinutes()).isEqualTo(third.durationMinutes());
        assertThat(corrected.amount()).isEqualByComparingTo(third.amount());
        assertThat(corrected.version()).isEqualTo(third.version()+1);
        assertThat(corrected.sequenceTitle()).isEqualTo("LeetCode 49");
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("3");
        assertThat(records.list(null,null,taskId,null)).hasSize(3);
        assertThat(sequences.state(taskId,date).mode()).isEqualTo("SELECT_BY_NUMBER");
        assertThat(sequences.state(taskId,date).current()).isNull();
        assertThat(sequences.state(taskId,date.plusDays(2)).dayItem().key()).isEqualTo("49");
    }
    @Test void arbitraryNumbersAttemptRetryEditAndDeleteKeepProgressCorrect() {
        convertNumbers(List.of());
        assertThat(sequences.state(taskId,date).actionable()).isTrue();
        var request=numbered(0,3000,null,UUID.randomUUID().toString());
        var attempt=records.create(request);
        assertThat(records.create(request).id()).isEqualTo(attempt.id());
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("0");
        var completed=records.create(numbered(0,3000,BigDecimal.ONE,UUID.randomUUID().toString()));
        var edited=records.update(completed.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.ONE,null,"原内容","原复盘",completed.version(),completed.sequenceItemId(),49));
        assertThat(edited.sequenceTitle()).isEqualTo("LeetCode 49");
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("1");
        assertThat(sequences.state(taskId,date.plusDays(1)).dayItem()).isNull();
        assertThat(sequences.state(taskId,date.plusDays(1)).actionable()).isTrue();
        assertThatThrownBy(()->records.update(completed.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.ONE,null,null,null,completed.version(),completed.sequenceItemId(),50))).isInstanceOf(BusinessException.class);
        records.delete(edited.id(),edited.version());
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("0");
    }
    @Test void numberedDayProgressAllowsSameDayAppendAndRejectsTokenChangesAndDeletedReplay() {
        convertNumbers(List.of());
        var snapshot=records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        var request=new DayProgressRequest(date,taskId,100,null,snapshot.revision(),UUID.randomUUID().toString(),null,null,49);
        var saved=records.recordDayProgress(request);
        assertThat(records.recordDayProgress(request).id()).isEqualTo(saved.id());
        assertThatThrownBy(()->records.recordDayProgress(new DayProgressRequest(date,taskId,100,null,snapshot.revision(),request.clientToken(),null,null,50))).isInstanceOf(BusinessException.class);
        var refreshed=records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        var second=records.recordDayProgress(new DayProgressRequest(date,taskId,100,null,refreshed.revision(),UUID.randomUUID().toString(),null,null,50));
        assertThat(second.amount()).isEqualByComparingTo("1");
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("2");
        records.delete(saved.id(),saved.version());
        assertThat(records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow().revision()).isNotEqualTo(snapshot.revision());
        assertThatThrownBy(()->records.create(numbered(0,49,BigDecimal.ONE,request.clientToken()))).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).extracting(StudyRecordView::id).containsExactly(second.id());
    }

    com.kaoyan.study.learning.dto.CompletedProblemNumbersRequest batch(int day,List<Integer> numbers,String token) {
        var snapshot=records.dayTasks(date.plusDays(day)).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        return new com.kaoyan.study.learning.dto.CompletedProblemNumbersRequest(date.plusDays(day),taskId,numbers,snapshot.revision(),token);
    }
    @Test void multipleProblemsAppendBeyondReferenceTargetAndEditsAndDeletesRecalculate() {
        convertNumbers(List.of());
        var request=batch(0,List.of(31,32),UUID.randomUUID().toString());
        var first=records.recordCompletedProblems(request);
        assertThat(records.recordCompletedProblems(request)).extracting(StudyRecordView::id)
                .containsExactlyElementsOf(first.stream().map(StudyRecordView::id).toList());
        records.recordCompletedProblems(batch(0,List.of(49,128,283),UUID.randomUUID().toString()));
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("5");
        var day=records.dayTasks(date).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        assertThat(day.completedAmount()).isEqualByComparingTo("5");
        assertThat(day.sequence().actionable()).isTrue();
        var checklist=today.today(date).sections().stream().flatMap(s->s.tasks().stream())
                .filter(t->t.id().equals(taskId) && t.kind().equals("LEARNING")).findFirst().orElseThrow();
        assertThat(checklist.actionable()).isTrue();
        assertThat(checklist.recordedAmount()).isEqualByComparingTo("5");
        assertThat(records.list(date.plusDays(1),date.plusDays(1),taskId,null)).isEmpty();
        assertThat(records.list(null,null,taskId,null)).allSatisfy(r->{
            assertThat(r.recordDate()).isEqualTo(date); assertThat(r.durationMinutes()).isNull();
            assertThat(r.amount()).isEqualByComparingTo("1");
        });
        var corrected=records.update(first.getFirst().id(),new StudyRecordUpdateRequest(taskId,null,null,date,
                BigDecimal.ONE,null,null,null,first.getFirst().version(),first.getFirst().sequenceItemId(),3000));
        assertThat(corrected.sequenceTitle()).isEqualTo("LeetCode 3000");
        records.delete(first.get(1).id(),first.get(1).version());
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("4");
        records.recordCompletedProblems(batch(1,List.of(32),UUID.randomUUID().toString()));
        assertThat(records.get(corrected.id()).recordDate()).isEqualTo(date);
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("5");
    }
    @Test void batchRetryRejectsChangedListsDatesAndRevokedOrEditedRecords() {
        convertNumbers(List.of());
        String token=UUID.randomUUID().toString();
        var request=batch(0,List.of(31,32),token);
        var saved=records.recordCompletedProblems(request);
        for (var changed:List.of(List.of(31),List.of(31,32,49),List.of(32,31),List.of(31,49)))
            assertThatThrownBy(()->records.recordCompletedProblems(batch(0,changed,token))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.recordCompletedProblems(batch(1,List.of(31,32),token))).isInstanceOf(BusinessException.class);
        var second=saved.get(1);
        var edited=records.update(second.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.ONE,
                null,null,null,second.version(),second.sequenceItemId(),49));
        assertThatThrownBy(()->records.recordCompletedProblems(request)).isInstanceOf(BusinessException.class);
        records.delete(edited.id(),edited.version());
        assertThatThrownBy(()->records.recordCompletedProblems(request)).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).hasSize(1);
    }
    @Test void duplicateProblemRollsBackEntireBatchAndFailedTokenCanRetry() {
        convertNumbers(List.of());
        records.create(numbered(0,49,BigDecimal.ONE,UUID.randomUUID().toString()));
        var request=batch(1,List.of(128,49),UUID.randomUUID().toString());
        assertThatThrownBy(()->records.recordCompletedProblems(request)).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).hasSize(1);
        var valid=new com.kaoyan.study.learning.dto.CompletedProblemNumbersRequest(request.date(),taskId,
                List.of(128,283),request.revision(),request.clientToken());
        assertThat(records.recordCompletedProblems(valid)).hasSize(2);
        assertThat(tasks.progress(taskId).completedAmount()).isEqualByComparingTo("3");
    }
    @Test void concurrentBatchesSerializeAndStaleRevisionsCannotAppend() throws Exception {
        convertNumbers(List.of());
        var request=batch(0,List.of(31,32),UUID.randomUUID().toString());
        var gate=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<List<Long>> write=()->{gate.await();return records.recordCompletedProblems(request).stream().map(StudyRecordView::id).toList();};
            var a=pool.submit(write);var b=pool.submit(write);gate.countDown();
            assertThat(a.get(20,TimeUnit.SECONDS)).isEqualTo(b.get(20,TimeUnit.SECONDS));
        }
        var left=batch(0,List.of(49),UUID.randomUUID().toString());
        var right=batch(0,List.of(128),UUID.randomUUID().toString());
        var gate2=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            var requests=List.of(left,right);
            var futures=requests.stream().map(r->pool.submit(()->{gate2.await();try {records.recordCompletedProblems(r);return true;} catch(BusinessException e){return false;}})).toList();
            gate2.countDown();
            assertThat(List.of(futures.get(0).get(20,TimeUnit.SECONDS),futures.get(1).get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThatThrownBy(()->records.recordCompletedProblems(new com.kaoyan.study.learning.dto.CompletedProblemNumbersRequest(
                date,taskId,List.of(283),request.revision(),UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).hasSize(3);
    }
    @Test void batchRejectsWrongModeInvalidNumbersAndDuplicates() {
        assertThatThrownBy(()->records.recordCompletedProblems(batch(0,List.of(31,32),UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        convertNumbers(List.of());
        for (var invalid:List.of(List.<Integer>of(),List.of(0),List.of(-1),List.of(31,31)))
            assertThatThrownBy(()->records.recordCompletedProblems(batch(0,invalid,UUID.randomUUID().toString()))).isInstanceOf(BusinessException.class);
        assertThat(records.list(null,null,taskId,null)).isEmpty();
    }
    @Test void batchHttpRequiresAuthenticationCsrfAndValidatesWholeList() {
        convertNumbers(List.of());
        var client=new com.kaoyan.study.support.HttpTestClient("http://127.0.0.1:"+port);
        String path="/api/learning/records/problem-numbers";
        var csrf=client.fetchCsrfToken();
        assertThat(client.post(path,"{}",csrf).status()).isEqualTo(401);
        assertThat(client.post("/api/auth/login","{\"username\":\"kaoyan\",\"password\":\"test-only-password\"}",csrf).status()).isEqualTo(200);
        var request=batch(0,List.of(31,32),UUID.randomUUID().toString());
        String body="{\"date\":\"%s\",\"taskId\":%d,\"problemNumbers\":[31,32],\"revision\":\"%s\",\"clientToken\":\"%s\"}"
                .formatted(date,taskId,request.revision(),request.clientToken());
        assertThat(client.post(path,body,null).status()).isEqualTo(403);
        csrf=client.fetchCsrfToken();
        for (String invalid:List.of("[]","[0]","[null]","[2147483648]","[31,31]"))
            assertThat(client.post(path,body.replace("[31,32]",invalid),csrf).status()).isEqualTo(400);
        assertThat(records.list(null,null,taskId,null)).isEmpty();
        var saved=client.post(path,body,csrf);
        assertThat(saved.status()).as(saved.body()).isEqualTo(201);
        assertThat(saved.number("$.length()").intValue()).isEqualTo(2);
        assertThat(client.post(path,body,csrf).status()).isEqualTo(201);
        assertThat(records.list(null,null,taskId,null)).hasSize(2);
    }
    @Test void numberedModeRejectsInvalidNumberFractionDateAndDuplicateProblem() {
        convertNumbers(List.of());
        assertThatThrownBy(()->records.create(numbered(0,0,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(numbered(0,-1,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(numbered(0,49,new BigDecimal("0.5"),null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(numbered(3,49,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->records.create(new StudyRecordCreateRequest(taskId,null,null,date,BigDecimal.ONE,null,null,null,null,items.get(0).id()))).isInstanceOf(BusinessException.class);
        records.create(numbered(0,49,BigDecimal.ONE,null));
        assertThatThrownBy(()->records.create(numbered(1,49,BigDecimal.ONE,null))).isInstanceOf(BusinessException.class);
    }
    @Test void conversionChecksStaleVersionsAndRollsBackAllCorrectionsOnFailure() {
        var first=complete(0,0);var second=complete(1,1);
        var preview=numberMode.preview(taskId);
        var invalid=List.of(new ProblemNumberModeService.Correction(first.id(),first.version(),49),new ProblemNumberModeService.Correction(second.id(),second.version()+1,50));
        assertThatThrownBy(()->numberMode.confirm(taskId,new ProblemNumberModeService.Confirm("自填题号",preview.version(),preview.workspaceRevision(),preview.revision(),invalid))).isInstanceOf(BusinessException.class);
        assertThat(records.get(first.id())).usingRecursiveComparison().ignoringFields("taskTitle").isEqualTo(first);
        assertThat(sequences.state(taskId,date).mode()).isEqualTo("COMPLETE_THEN_ADVANCE");
        complete(2,2);
        assertThatThrownBy(()->numberMode.confirm(taskId,new ProblemNumberModeService.Confirm("自填题号",preview.version(),preview.workspaceRevision(),preview.revision(),List.of()))).isInstanceOf(BusinessException.class);
    }
    @Test void v7ImportsAnEmptyNumberDirectoryAndRejectsOldVersions() {
        String flexible=source.replace("\"schemaVersion\":2","\"schemaVersion\":7").replace("COMPLETE_THEN_ADVANCE","SELECT_BY_NUMBER");
        int start=flexible.indexOf("\"items\":[");int end=flexible.indexOf("]",start);
        flexible=flexible.substring(0,start)+"\"items\":[]"+flexible.substring(end+1);
        flexible=flexible.replace("\"planId\":\"", "\"planId\":\"new-");
        var preview=imports.preview(flexible);
        imports.confirm(new PlanImportRequest(flexible,preview.previewToken()));
        var imported=tasks.list(subjectId,null,null).stream().filter(t->!t.getId().equals(taskId)).findFirst().orElseThrow();
        assertThat(sequences.state(imported.getId(),date).items()).isEmpty();
        assertThat(sequences.state(imported.getId(),date).actionable()).isTrue();
        final String old=flexible.replace("\"schemaVersion\":7","\"schemaVersion\":6");
        assertThatThrownBy(()->imports.preview(old)).isInstanceOf(BusinessException.class);
    }

}
