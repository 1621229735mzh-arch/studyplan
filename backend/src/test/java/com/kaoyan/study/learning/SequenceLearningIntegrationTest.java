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
    @Autowired PlanImportService imports;
    @Autowired StudyRecordService records;
    @Autowired SequenceLearningService sequences;
    @Autowired TaskService tasks;
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

}
