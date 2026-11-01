package com.kaoyan.study.plan;

import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.memo.dto.MemoRequest;
import com.kaoyan.study.memo.service.MemoService;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import com.kaoyan.study.progress.service.CatalogService;
import com.kaoyan.study.progress.dto.CatalogContracts.*;
import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest(properties={"app.preset-account.username=context-test", "app.preset-account.password=test-only-password",
        "app.review.automatic-reconciliation-enabled=false"})
@AutoConfigureMockMvc
@Transactional
class PlanningContextExportIntegrationTest {
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) { MySqlTestSupport.registerDataSource(r); }
    @Autowired PlanningContextExportService exports;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired TaskService tasks;
    @Autowired TaskActivityService activities;
    @Autowired StudyRecordService records;
    @Autowired DailyPlanService daily;
    @Autowired ReviewService reviews;
    @Autowired MemoService memos;
    @Autowired StudyDay day;
    @Autowired CatalogService catalog;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    private Long taskId, subjectId, unitId;
    private LocalDate date;

    @BeforeEach void setup() {
        var suffix=UUID.randomUUID().toString().substring(0,8);
        subjectId=subjects.create(new SubjectRequest("上下文-"+suffix,"专业课",null,0,true)).getId();
        unitId=units.create(new StudyUnitRequest("讲-"+suffix,0,true)).getId();
        taskId=tasks.create(new TaskCreateRequest(subjectId,null,"原任务 | <b>内容</b>",unitId,BigDecimal.TEN)).getId();
        date=day.today();
    }
    private StudyRecordView record(LocalDate date,BigDecimal amount,String note) {
        return records.create(new StudyRecordCreateRequest(taskId,null,null,date,amount,null,"真实学习",note,UUID.randomUUID().toString()));
    }
    @Test void preservesIdsReferenceDatesAndCompleteHistoryWhileSummarizingOnlyThirtyDays() {
        var old=record(date.minusDays(30),BigDecimal.ONE,"旧反馈");
        var recent=record(date.minusDays(29),BigDecimal.valueOf(2),"需要订正 | <script>内容</script>\n第二行");
        daily.addItem(date.plusDays(10),new DailyItemCreateRequest(taskId,BigDecimal.TEN,null));
        var memo=memos.create(new MemoRequest(subjectId,"待解决：极限的证明",date,"OPEN",0L));
        memos.create(new MemoRequest(subjectId,"已处理问题",date,"DONE",0L));
        var document=new Document(1,List.of(new CourseInput("course-test","CS408","操作系统","原课程",subjectId,unitId,
                List.of(new NodeInput("chapter",null,"目录内容",BigDecimal.TEN,0,List.of(taskId))))));
        catalog.confirm(catalog.preview(json.writeValueAsString(document)).previewToken());
        var result=exports.export(); var s=result.snapshot();
        assertThat(s.recentFrom()).isEqualTo(date.minusDays(29));
        assertThat(s.feedback().studyRecordIds()).contains(recent.id()).doesNotContain(old.id());
        assertThat(s.facts().studyRecords()).extracting(StudyRecordView::id).contains(old.id(),recent.id());
        assertThat(s.facts().tasks()).filteredOn(t->t.id().equals(taskId)).singleElement().satisfies(t->{
            assertThat(t.actual().firstStudyAmount()).isEqualByComparingTo("3"); assertThat(t.id()).isEqualTo(taskId);
        });
        assertThat(s.facts().referencePlan().entries()).anyMatch(e->e.taskId().equals(taskId) && e.date().equals(date.plusDays(10)) && e.completedAmount().signum()==0);
        assertThat(s.facts().openMemos()).extracting(m->m.id()).containsExactly(memo.getId());
        assertThat(s.facts().progress().courses()).singleElement().satisfies(c->{
            assertThat(c.id()).isPositive(); assertThat(c.completedAmount()).isEqualByComparingTo("3");
            assertThat(c.nodes().getFirst().taskIds()).containsExactly(taskId);
        });
        assertThat(result.markdown()).contains("原任务 &#124; &lt;b&gt;内容&lt;/b&gt;","需要订正 &#124; &lt;script&gt;内容&lt;/script&gt;<br>第二行",s.dataVersion(),"旧反馈");
    }
    @Test void fingerprintIsStableUntilAnEditOrDeleteAndExportDoesNotWriteFacts() {
        var record=record(date,BigDecimal.ONE,"原复盘");
        var before=databaseFacts(); var first=exports.export(); var second=exports.export();
        assertThat(databaseFacts()).isEqualTo(before);
        assertThat(first.snapshot().dataVersion()).isEqualTo(second.snapshot().dataVersion()).hasSize(64);
        var changed=records.update(record.id(),new StudyRecordUpdateRequest(taskId,null,null,date,BigDecimal.valueOf(2),null,"真实学习","改复盘",record.version()));
        var updated=exports.export(); assertThat(updated.snapshot().dataVersion()).isNotEqualTo(first.snapshot().dataVersion());
        records.delete(changed.id(),changed.version());
        var deleted=exports.export(); assertThat(deleted.snapshot().dataVersion()).isNotEqualTo(updated.snapshot().dataVersion());
        assertThat(deleted.snapshot().facts().studyRecords()).noneMatch(r->r.id().equals(record.id()));
        assertThat(deleted.snapshot().facts().tasks()).filteredOn(t->t.id().equals(taskId)).singleElement()
                .satisfies(t->assertThat(t.actual().firstStudyAmount()).isZero());
    }
    @Test void reviewQuantitiesRemainSeparateAndMissingCountsStayInOriginalRecords() {
        activities.create(taskId,new TaskActivityService.Activity("COUNT_REVIEW","单词复习",0));
        record(date,BigDecimal.valueOf(70),"复习70词");
        var result=exports.export().snapshot();
        assertThat(result.facts().tasks()).filteredOn(t->t.id().equals(taskId)).singleElement().satisfies(t->{
            assertThat(t.actual().firstStudyAmount()).isZero(); assertThat(t.actual().reviewAmount()).isEqualByComparingTo("70");
        });
        assertThat(result.feedback().quantities()).filteredOn(q->subjectId.equals(q.subjectId())).singleElement().satisfies(q->{
            assertThat(q.firstStudyAmount()).isZero(); assertThat(q.reviewAmount()).isEqualByComparingTo("70");
        });
    }
    @Test void legacyReviewWithoutQuantityIsUnknownRatherThanFabricatedZero() {
        activities.create(taskId,new TaskActivityService.Activity("COUNT_REVIEW","历史复习",0));
        jdbc.update("INSERT INTO study_record(task_id,subject_id,unit_id,record_date,activity_completed) VALUES(?,?,?,?,true)",taskId,subjectId,unitId,date);
        var snapshot=exports.export().snapshot();
        assertThat(snapshot.facts().tasks()).filteredOn(t->t.id().equals(taskId)).singleElement().satisfies(t->{
            assertThat(t.actual().reviewAmount()).isNull(); assertThat(t.actual().unknownQuantityRecords()).isEqualTo(1);
        });
        assertThat(snapshot.feedback().quantities()).filteredOn(q->subjectId.equals(q.subjectId())).singleElement()
                .satisfies(q->assertThat(q.unknownReviewRecords()).isEqualTo(1));
    }
    @Test void weaknessUsesLatestRealFeedbackAndDoesNotTreatNoFeedbackAsMastery() {
        record(date.minusDays(4),BigDecimal.ONE,null);
        var item=reviews.addToReview(new ReviewItemCreateRequest(taskId,null,null,null));
        assertThat(exports.export().snapshot().feedback().weaknesses()).isEmpty();
        reviews.recordReview(new ReviewRecordCreateRequest(item.getId(),date.minusDays(2),MasteryResult.FORGOT,null,"不会证明",null,UUID.randomUUID().toString(),item.getVersion()));
        var weak=exports.export(); assertThat(weak.snapshot().feedback().weaknesses()).singleElement().satisfies(w->{
            assertThat(w.reviewItemId()).isEqualTo(item.getId()); assertThat(w.result()).isEqualTo("FORGOT");
        });
        reviews.recordReview(new ReviewRecordCreateRequest(item.getId(),date,MasteryResult.MASTERED,null,"独立完成",null,UUID.randomUUID().toString(),reviews.get(item.getId()).getVersion()));
        assertThat(exports.export().snapshot().feedback().weaknesses()).isEmpty();
    }
    @Test void endpointRequiresAuthenticationAndNeverCachesPersonalContext() throws Exception {
        mvc.perform(get("/api/plan/context-export")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/plan/context-export").with(user("context-test")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.snapshot.documentType").value("408doing.planning-context"))
                .andExpect(jsonPath("$.snapshot.schemaVersion").value(1))
                .andExpect(jsonPath("$.snapshot.facts.tasks").isArray()).andExpect(jsonPath("$.markdown").isString())
                .andExpect(jsonPath("$.snapshot.password").doesNotExist()).andExpect(jsonPath("$.snapshot.clientToken").doesNotExist());
    }
    private Map<String,List<Map<String,Object>>> databaseFacts() {
        var result=new TreeMap<String,List<Map<String,Object>>>();
        for(var table:List.of("task","daily_plan_item","study_record","review_item","review_record","memo","review_fsrs_state","review_auto_policy"))
            result.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY 1"));
        return result;
    }
}
