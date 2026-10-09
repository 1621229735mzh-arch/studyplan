package com.kaoyan.study.plan;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import com.kaoyan.study.support.HttpTestClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlanWorkspaceIntegrationTest {
    @Autowired PlanImportService imports;
    @Autowired PlanWorkspaceService workspace;
    @Autowired DailyPlanService daily;
    @Autowired TaskService tasks;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired StudyRecordService records;
    @Autowired JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    final JsonMapper json=JsonMapper.builder().build();
    Long subjectId,unitId;
    String source;
    LocalDate date=LocalDate.of(2027,1,4);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username",()->"kaoyan");
        registry.add("app.preset-account.password",()->"test-only-password");
    }
    @BeforeEach
    void fixture() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        String subject="规划测试-"+suffix;
        subjectId=subjects.create(new SubjectRequest(subject,null,null,0,true)).getId();
        String unit="讲-"+suffix;
        unitId=units.create(new StudyUnitRequest(unit,0,true)).getId();
        String exercise="题-"+suffix;
        units.create(new StudyUnitRequest(exercise,0,true));
        source=Files.readString(Path.of("../docs/business/plan-import-example.json"))
                .replace("数学一",subject).replace("\"讲\"","\""+unit+"\"").replace("\"题\"","\""+exercise+"\"")
                .replace("math-foundation-example-2027-01",UUID.randomUUID().toString());
    }
    @Test
    void previewDoesNotWriteAndConfirmationAppendsToDailyFacts() {
        var existing=tasks.create(new TaskCreateRequest(subjectId,null,"已有安排",unitId,BigDecimal.TEN));
        daily.addItem(date,new DailyItemCreateRequest(existing.getId(),BigDecimal.ONE,null));
        long before=count("task");
        var preview=imports.preview(source);
        assertThat(preview.itemCount()).isEqualTo(4);
        assertThat(count("task")).isEqualTo(before);
        imports.confirm(new PlanImportRequest(source,preview.previewToken()));
        assertThat(count("task")).isEqualTo(before+2);
        assertThat(daily.list(date)).anyMatch(d -> d.taskId().equals(existing.getId()));
        var view=workspace.read(null,null,subjectId);
        assertThat(view.entries()).hasSize(5);
        assertThat(view.months()).hasSize(1);
        assertThat(view.months().getFirst().amounts()).hasSize(2);
        assertThat(view.months().getFirst().amounts()).allMatch(a -> a.completedAmount().signum()==0);
        assertThat(daily.list(date.plusDays(2)).stream().filter(d -> subjectId.equals(d.subjectId()))).isEmpty();
    }
    @Test
    void duplicatePlanAndSameContentWithAnotherIdAreRejected() {
        var p=imports.preview(source); imports.confirm(new PlanImportRequest(source,p.previewToken()));
        long before=count("task");
        assertThatThrownBy(()->imports.confirm(new PlanImportRequest(source,p.previewToken()))).isInstanceOf(ConflictException.class);
        ObjectNode changed=(ObjectNode)json.readTree(source); changed.put("planId",UUID.randomUUID().toString());
        assertThatThrownBy(()->imports.preview(json.writeValueAsString(changed))).isInstanceOf(ConflictException.class);
        String decimalVariant=json.writeValueAsString(changed)
                .replaceAll("(\"(?:plannedAmount|amount)\":)([0-9]+)(?=[,}])", "$1$2.0");
        assertThatThrownBy(()->imports.preview(decimalVariant)).isInstanceOf(ConflictException.class);
        assertThat(count("task")).isEqualTo(before);
    }
    @Test
    void changedPlanOrFileRequiresNewPreview() {
        var p=imports.preview(source);
        workspace.quickAdd(date,quick("新增内容",UUID.randomUUID().toString()));
        assertThatThrownBy(()->imports.confirm(new PlanImportRequest(source,p.previewToken()))).isInstanceOf(ConflictException.class);
        String token=imports.preview(source).previewToken();
        assertThatThrownBy(()->imports.confirm(new PlanImportRequest(source.replace("极限入门","极限与连续"),token)))
                .isInstanceOf(ConflictException.class);
    }
    @Test
    void databaseFailureRollsBackBatchTasksAndGoals() {
        var p=imports.preview(source);
        long taskBefore=count("task"), goalBefore=count("stage_goal"), importBefore=count("plan_import");
        // 真正 MySQL 故障：第二个任务插入失败，应回滚此前的批次、目标和第一个任务。
        jdbc.execute("CREATE TRIGGER planning_test_failure BEFORE INSERT ON task FOR EACH ROW BEGIN IF NEW.subject_id="+subjectId+" AND NEW.title='极限配套练习' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test failure'; END IF; END");
        try {
            assertThatThrownBy(()->imports.confirm(new PlanImportRequest(source,p.previewToken()))).isInstanceOf(RuntimeException.class);
            assertThat(count("task")).isEqualTo(taskBefore);
            assertThat(count("stage_goal")).isEqualTo(goalBefore);
            assertThat(count("plan_import")).isEqualTo(importBefore);
        } finally { jdbc.execute("DROP TRIGGER planning_test_failure"); }
        imports.confirm(new PlanImportRequest(source,p.previewToken()));
    }
    @Test
    void concurrentConfirmationsCommitOnlyOnce() throws Exception {
        var p=imports.preview(source); var request=new PlanImportRequest(source,p.previewToken());
        var gate=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> submit=()-> { gate.await(); try { imports.confirm(request); return true; } catch (ConflictException ex) { return false; } };
            var first=pool.submit(submit); var second=pool.submit(submit); gate.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        assertThat(tasks.list(subjectId,null,null)).hasSize(2);
    }
    @ParameterizedTest
    @ValueSource(strings={"unknown","duplicateField","wrongDate","missingDay","reference","quantity","stringNumber","nullItem","missingMonths","missingWeeks"})
    void malformedPlansNeverWrite(String kind) {
        ObjectNode root=(ObjectNode)json.readTree(source);
        switch(kind) {
            case "unknown" -> root.put("extra",true);
            case "wrongDate" -> root.put("startDate","2027-02-30");
            case "missingDay" -> ((tools.jackson.databind.node.ArrayNode)root.get("days")).remove(1);
            case "reference" -> ((ObjectNode)root.get("days").get(0).get("items").get(0)).put("taskKey","missing");
            case "quantity" -> ((ObjectNode)root.get("tasks").get(0)).put("plannedAmount",3);
            case "stringNumber" -> ((ObjectNode)root.get("tasks").get(0)).put("plannedAmount","2");
            case "nullItem" -> ((tools.jackson.databind.node.ArrayNode)root.get("days").get(0).get("items")).addNull();
            case "missingMonths" -> root.putArray("months");
            case "missingWeeks" -> root.putArray("weeks");
            default -> { }
        }
        String invalid=kind.equals("duplicateField") ? source.replace("\"schemaVersion\": 1","\"schemaVersion\": 1, \"schemaVersion\": 1") : json.writeValueAsString(root);
        long before=count("plan_import");
        assertThatThrownBy(()->imports.preview(invalid)).isInstanceOf(BusinessException.class);
        assertThat(count("plan_import")).isEqualTo(before);
        assertThat(tasks.list(subjectId,null,null)).isEmpty();
    }
    @Test
    void creationCannotOverwriteExistingItemOrBypassManagedSources() {
        workspace.quickAdd(date,quick("保留已有安排",UUID.randomUUID().toString()));
        var task=tasks.list(subjectId,null,null).getFirst();
        assertThatThrownBy(()->daily.addItem(date,new DailyItemCreateRequest(task.getId(),BigDecimal.TEN,null)))
                .isInstanceOf(ConflictException.class);
        assertThat(daily.list(date).stream().filter(i -> i.taskId().equals(task.getId())).findFirst().orElseThrow().plannedAmount())
                .isEqualByComparingTo("1");
        for (var source:List.of(com.kaoyan.study.plan.entity.PlanSource.REVIEW,com.kaoyan.study.plan.entity.PlanSource.IMPORT))
            assertThatThrownBy(()->daily.addItem(date.plusDays(1),new DailyItemCreateRequest(task.getId(),BigDecimal.ONE,source)))
                    .isInstanceOf(ConflictException.class);
    }
    @Test
    void quickAddIsIdempotentAndInvalidUnitLeavesNoOrphanTask() {
        var r=quick("当天直接添加",UUID.randomUUID().toString());
        workspace.quickAdd(date,r); workspace.quickAdd(date,r);
        assertThat(tasks.list(subjectId,null,null)).hasSize(1);
        assertThatThrownBy(()->workspace.quickAdd(date,new QuickDayRequest(null,subjectId,Long.MAX_VALUE,"无效单位",BigDecimal.ONE,null,UUID.randomUUID().toString())))
                .isInstanceOf(BusinessException.class);
        assertThat(tasks.list(subjectId,null,null)).hasSize(1);
    }
    @Test
    void reorderEditAndDeleteCheckVersionsAndKeepRecords() {
        workspace.quickAdd(date,quick("第一项",UUID.randomUUID().toString()));
        workspace.quickAdd(date,quick("第二项",UUID.randomUUID().toString()));
        var rows=daily.list(date); var reversed=new ArrayList<>(rows); Collections.reverse(reversed);
        var request=new ReorderRequest(reversed.stream().map(i -> new ReorderRequest.Item(i.id(),i.version())).toList());
        var reordered=workspace.reorder(date,request);
        assertThat(reordered).extracting(DailyPlanItemView::id).containsExactlyElementsOf(reversed.stream().map(DailyPlanItemView::id).toList());
        assertThatThrownBy(()->workspace.reorder(date,request)).isInstanceOf(ConflictException.class);
        var appended=workspace.quickAdd(date,quick("追加到最后",UUID.randomUUID().toString()));
        assertThat(appended.getLast().taskTitle()).isEqualTo("追加到最后");
        var first=reordered.stream().filter(i->subjectId.equals(i.subjectId())).findFirst().orElseThrow();
        var record=records.create(new StudyRecordCreateRequest(first.taskId(),null,null,date,BigDecimal.ONE,30,"已学",null,UUID.randomUUID().toString()));
        daily.adjust(date,first.id(),new DailyItemAdjustRequest(BigDecimal.TEN,first.version(),"测试调整"));
        assertThatThrownBy(()->daily.removeItem(date,first.id(),first.version())).isInstanceOf(ConflictException.class);
        var updated=daily.list(date).stream().filter(i->i.id().equals(first.id())).findFirst().orElseThrow();
        assertThat(workspace.read(date,date,subjectId).entries()).anyMatch(e->e.plannedAmount().compareTo(BigDecimal.TEN)==0 && e.completedAmount().compareTo(BigDecimal.ONE)==0);
        daily.removeItem(date,first.id(),updated.version());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM study_record WHERE id=? AND deleted_at IS NULL",Long.class,record.id())).isEqualTo(1);
    }
    @Test
    void missingExpectedTimeStaysUnknownAndMultipleSourcesDoNotDoubleCountProgress() {
        workspace.quickAdd(date,quick("多来源任务",UUID.randomUUID().toString()));
        var task=tasks.list(subjectId,null,null).getFirst();
        daily.addItem(date,new DailyItemCreateRequest(task.getId(),BigDecimal.ONE,com.kaoyan.study.plan.entity.PlanSource.WEEKLY,10));
        var r=records.create(new StudyRecordCreateRequest(task.getId(),null,null,date,BigDecimal.ONE,20,"已学",null,null));
        var entry=workspace.read(date,date,subjectId).entries().getFirst();
        assertThat(entry.plannedAmount()).isEqualByComparingTo("2");
        assertThat(entry.completedAmount()).isEqualByComparingTo("1");
        assertThat(entry.estimatedMinutes()).isNull();
        records.delete(r.id(),r.version());
        assertThat(workspace.read(date,date,subjectId).entries().getFirst().completedAmount()).isEqualByComparingTo("0");
    }
    @Test
    void httpRequiresAuthenticationCsrfAndReturnsFieldErrors() {
        var client=new HttpTestClient("http://127.0.0.1:"+port);
        assertThat(client.get("/api/plan/workspace").status()).isEqualTo(401);
        String csrf=client.fetchCsrfToken();
        assertThat(client.post("/api/auth/login","{\"username\":\"kaoyan\",\"password\":\"test-only-password\"}",csrf).status()).isEqualTo(200);
        csrf=client.fetchCsrfToken();
        assertThat(client.post("/api/plan/imports/preview",json.writeValueAsString(new PlanImportRequest(source,null)),null).status()).isEqualTo(403);
        assertThat(client.post("/api/plan/imports/preview",json.writeValueAsString(new PlanImportRequest(source,null)),csrf).status()).isEqualTo(200);
        assertThat(client.post("/api/plan/imports/preview","{\"document\":\"{}\"}",csrf).status()).isEqualTo(400);
        assertThat(client.get("/api/plan/workspace?subjectId="+subjectId).status()).isEqualTo(200);
    }
    private QuickDayRequest quick(String title,String token) { return new QuickDayRequest(null,subjectId,unitId,title,BigDecimal.ONE,null,token); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class); }
}
