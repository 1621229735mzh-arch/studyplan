package com.kaoyan.study.progress;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.*;
import com.kaoyan.study.learning.service.*;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.progress.dto.CatalogContracts.*;
import com.kaoyan.study.progress.service.CatalogService;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CatalogIntegrationTest {
    @Autowired CatalogService catalog;
    @Autowired StudyRecordService records;
    @Autowired RecordArchiveService archive;
    @Autowired TaskService tasks;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired JdbcTemplate jdbc;
    Long subjectId,unitId,taskId;
    String courseKey;
    final JsonMapper json=JsonMapper.builder().build();
    final LocalDate date=LocalDate.of(2031,4,7);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username",()->"kaoyan");
        registry.add("app.preset-account.password",()->"test-only-password");
    }
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        subjectId=subjects.create(new SubjectRequest("数学目录-"+suffix,"公共课",null,0,true)).getId();
        unitId=units.create(new StudyUnitRequest("讲目录-"+suffix,0,true)).getId();
        taskId=tasks.create(new TaskCreateRequest(subjectId,null,"第1讲",unitId,new BigDecimal("20"))).getId();
        courseKey="course-"+suffix;
    }
    NodeInput leaf(String key,String amount,List<Long> taskIds) {return new NodeInput(key,null,key,new BigDecimal(amount),0,taskIds);}
    Document doc(NodeInput... nodes) {return new Document(1,List.of(new CourseInput(courseKey,"MATH","高等数学","测试完整课程",subjectId,unitId,List.of(nodes))));}
    Library apply(Document document) {return catalog.confirm(catalog.preview(json.writeValueAsString(document)).previewToken());}
    CourseView course() {return catalog.library().courses().stream().filter(c->c.key().equals(courseKey)).findFirst().orElseThrow();}
    StudyRecordView record(Long task,String amount,Integer minutes,LocalDate day) {
        return records.create(new StudyRecordCreateRequest(task,task==null?subjectId:null,task==null?unitId:null,day,new BigDecimal(amount),minutes,"学习内容",null,UUID.randomUUID().toString()));
    }

    @Test void fullDenominatorAndAutomaticTaskHistoryDoNotCreateRecordsOrChangePlans() {
        var r=record(taskId,"10",60,date);
        var initial=doc(leaf("lesson1","20",List.of(taskId)),leaf("unplanned","60",List.of()));
        long taskCount=jdbc.queryForObject("SELECT COUNT(*) FROM task",Long.class);
        long recordCount=jdbc.queryForObject("SELECT COUNT(*) FROM study_record",Long.class);
        apply(initial);
        assertThat(course().totalAmount()).isEqualByComparingTo("80");
        assertThat(course().completedAmount()).isEqualByComparingTo("10");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM task",Long.class)).isEqualTo(taskCount);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM study_record",Long.class)).isEqualTo(recordCount);
        assertThat(catalog.library().unclassified()).noneMatch(x->x.id().equals(r.id()));
        record(taskId,"2",15,date.plusDays(1));
        assertThat(course().completedAmount()).isEqualByComparingTo("12");
    }
    @Test void appendIsConfirmedAndRetryDoesNotDuplicateAndKeepsOldNodesAndAmount() {
        apply(doc(leaf("first","80",List.of(taskId)))); record(taskId,"10",30,date);
        var preview=catalog.preview(json.writeValueAsString(doc(leaf("added","20",List.of()))));
        assertThat(preview.changes().getFirst().previousTotal()).isEqualByComparingTo("80");
        assertThat(preview.changes().getFirst().nextTotal()).isEqualByComparingTo("100");
        assertThat(course().totalAmount()).isEqualByComparingTo("80");
        catalog.confirm(preview.previewToken()); catalog.confirm(preview.previewToken());
        assertThat(course().nodes()).hasSize(2);
        assertThat(course().totalAmount()).isEqualByComparingTo("100");
        assertThat(course().completedAmount()).isEqualByComparingTo("10");
    }
    @Test void explicitClassificationOverridesTaskMappingWithoutDoublingSubjectAndEditsRecalculate() {
        apply(doc(leaf("task","80",List.of(taskId)),leaf("manual","20",List.of())));
        var r=record(taskId,"2",20,date);
        Long node=course().nodes().stream().filter(n->n.key().equals("manual")).findFirst().orElseThrow().id();
        var library=catalog.library();
        catalog.assign(r.id(),new AssignRequest(node,r.version(),library.revision()));
        assertThat(course().nodes().stream().filter(n->n.key().equals("task")).findFirst().orElseThrow().completedAmount()).isZero();
        assertThat(course().completedAmount()).isEqualByComparingTo("2");
        assertThat(catalog.library().cumulative().stream().filter(q->q.subjectId().equals(subjectId)).findFirst().orElseThrow().amount()).isEqualByComparingTo("2");
        var changed=records.update(r.id(),new StudyRecordUpdateRequest(taskId,null,null,date,new BigDecimal("3.123456"),25,"改后内容",null,r.version()));
        assertThat(course().recordedAmount()).isEqualByComparingTo("3.123456");
        records.delete(changed.id(),changed.version());
        assertThat(course().recordedAmount()).isZero();
        assertThat(course().completedAmount()).isZero();
    }
    @Test void unclassifiedRecordCountsBeforeAndAfterAssignmentButWrongUnitsAreRejected() {
        apply(doc(leaf("first","80",List.of())));
        var r=record(null,"2",null,date);
        assertThat(catalog.library().unclassified()).anyMatch(x->x.id().equals(r.id()));
        catalog.assign(r.id(),new AssignRequest(course().nodes().getFirst().id(),r.version(),catalog.library().revision()));
        assertThat(course().completedAmount()).isEqualByComparingTo("2");
        Long other=units.create(new StudyUnitRequest("题目录-"+UUID.randomUUID().toString().substring(0,8),0,true)).getId();
        var wrong=records.create(new StudyRecordCreateRequest(null,subjectId,other,date,BigDecimal.ONE,null,null,null,UUID.randomUUID().toString()));
        assertThatThrownBy(()->catalog.assign(wrong.id(),new AssignRequest(course().nodes().getFirst().id(),wrong.version(),catalog.library().revision()))).isInstanceOf(BusinessException.class);
        assertThat(catalog.library().unclassified()).anyMatch(x->x.id().equals(wrong.id()));
    }
    @Test void repeatedFirstLessonCannotFinishUnlearnedSecondLesson() {
        apply(doc(leaf("first","1",List.of(taskId)),leaf("second","1",List.of())));
        record(taskId,"5",30,date);
        assertThat(course().recordedAmount()).isEqualByComparingTo("5");
        assertThat(course().completedAmount()).isEqualByComparingTo("1");
        assertThat(course().totalAmount()).isEqualByComparingTo("2");
    }
    @Test void linkingAndClearingTaskOwnershipMovesHistoryAndChecksCatalogRevision() {
        apply(doc(leaf("first","80",List.of())));
        var r=record(taskId,"2",20,date); Long node=course().nodes().getFirst().id();
        long revision=catalog.library().revision();
        catalog.linkTasks(node,new TaskLinkRequest(List.of(taskId),revision));
        assertThat(course().completedAmount()).isEqualByComparingTo("2");
        assertThat(catalog.library().unclassified()).noneMatch(x->x.id().equals(r.id()));
        catalog.linkTasks(node,new TaskLinkRequest(List.of(),catalog.library().revision()));
        assertThat(course().completedAmount()).isZero();
        assertThat(catalog.library().unclassified()).anyMatch(x->x.id().equals(r.id()));
        assertThatThrownBy(()->catalog.linkTasks(node,new TaskLinkRequest(List.of(taskId),revision))).isInstanceOf(ConflictException.class);
    }
    @Test void staleRecordAndCatalogVersionsCannotReclassify() {
        apply(doc(leaf("first","80",List.of())));
        var r=record(null,"1",10,date); var version=catalog.library().revision();
        var changed=records.update(r.id(),new StudyRecordUpdateRequest(null,subjectId,unitId,date,BigDecimal.TEN,10,null,null,r.version()));
        assertThatThrownBy(()->catalog.assign(changed.id(),new AssignRequest(course().nodes().getFirst().id(),r.version(),version))).isInstanceOf(ConflictException.class);
        assertThat(course().recordedAmount()).isZero();
    }
    @Test void changedTaskContextAndExpiredPreviewAreRejectedBeforeAnyDirectoryWrite() {
        var preview=catalog.preview(json.writeValueAsString(doc(leaf("first","80",List.of(taskId)))));
        jdbc.update("UPDATE task SET version=version+1 WHERE id=?",taskId);
        assertThatThrownBy(()->catalog.confirm(preview.previewToken())).isInstanceOf(ConflictException.class);
        assertThat(catalog.library().courses()).noneMatch(c->c.key().equals(courseKey));
        var second=catalog.preview(json.writeValueAsString(doc(leaf("first","80",List.of(taskId)))));
        jdbc.update("UPDATE progress_catalog_preview SET expires_at=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE token=?",second.previewToken());
        assertThatThrownBy(()->catalog.confirm(second.previewToken())).isInstanceOf(ConflictException.class);
    }
    @Test void globalRevisionInvalidatesOldPreviewAndDuplicateTaskAcrossCoursesCannotPartiallyWrite() {
        var old=catalog.preview(json.writeValueAsString(doc(leaf("first","80",List.of()))));
        apply(doc(leaf("newer","20",List.of())));
        assertThatThrownBy(()->catalog.confirm(old.previewToken())).isInstanceOf(ConflictException.class);
        var c1=new CourseInput("dup1","MATH","高数","一",subjectId,unitId,List.of(leaf("a","1",List.of(taskId))));
        var c2=new CourseInput("dup2","MATH","高数","二",subjectId,unitId,List.of(leaf("b","1",List.of(taskId))));
        assertThatThrownBy(()->catalog.preview(json.writeValueAsString(new Document(1,List.of(c1,c2))))).isInstanceOf(BusinessException.class);
        assertThat(catalog.library().courses()).noneMatch(c->c.key().equals("dup1") || c.key().equals("dup2"));
    }
    @Test void editedTaskAssociationInvalidatesExplicitRecordTarget() {
        apply(doc(leaf("first","80",List.of())));
        var r=record(null,"2",30,date);
        catalog.assign(r.id(),new AssignRequest(course().nodes().getFirst().id(),r.version(),catalog.library().revision()));
        records.update(r.id(),new StudyRecordUpdateRequest(taskId,null,null,date,new BigDecimal("2"),30,null,null,r.version()));
        assertThat(course().completedAmount()).isZero();
        assertThat(catalog.library().unclassified()).anyMatch(x->x.id().equals(r.id()));
    }
    @Test void archiveGroupsCourseSessionsAndSplitsCrossMonthWeekWithMissingTimeAndSeparateUnits() {
        apply(doc(leaf("first","80",List.of(taskId))));
        LocalDate first=LocalDate.of(2031,4,30),second=first.plusDays(1);
        record(taskId,"0.123456",10,first); record(taskId,"1",null,first); record(taskId,"2",20,second);
        var result=archive.archive(first,second,subjectId,null);
        assertThat(result.months()).hasSize(2); assertThat(result.stats().learningDays()).isEqualTo(2);
        assertThat(result.stats().studyMinutes()).isEqualTo(30); assertThat(result.stats().missingDurationRecords()).isEqualTo(1);
        var april=result.months().stream().filter(m->m.month().equals("2031-04")).findFirst().orElseThrow();
        var group=april.weeks().getFirst().days().getFirst().groups().getFirst();
        assertThat(group.records()).hasSize(2); assertThat(group.amount()).isEqualByComparingTo("1.123456");
        assertThat(group.courseId()).isEqualTo(course().id());
        assertThat(april.weeks().getFirst().to()).isEqualTo(first);
        Long other=units.create(new StudyUnitRequest("页目录-"+UUID.randomUUID().toString().substring(0,8),0,true)).getId();
        records.create(new StudyRecordCreateRequest(null,subjectId,other,first,new BigDecimal("50"),null,null,null,UUID.randomUUID().toString()));
        assertThat(archive.archive(first,second,subjectId,null).stats().amounts()).hasSize(2);
    }
    @Test void badDirectoryAndJsonTypesDoNotCreateAnyCourse() {
        assertThatThrownBy(()->catalog.preview(json.writeValueAsString(doc(new NodeInput("cycle","cycle","循环",BigDecimal.ONE,0,List.of()))))).isInstanceOf(BusinessException.class);
        var source=json.writeValueAsString(doc(leaf("a","1",List.of())));
        assertThatThrownBy(()->catalog.preview(source.replace("\"schemaVersion\":1","\"schemaVersion\":\"1\""))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->catalog.preview(source.replace("\"title\":\"测试完整课程\"","\"title\":42"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->catalog.preview(source.replace("\"schemaVersion\":1","\"schemaVersion\":1,\"schemaVersion\":1"))).isInstanceOf(BusinessException.class);
        assertThat(catalog.library().courses()).noneMatch(c->c.key().equals(courseKey));
    }
    @Test void containerConversionCannotDiscardTaskHistoryAndWrongRangeIsRejected() {
        apply(doc(leaf("first","80",List.of(taskId)))); record(taskId,"1",null,date);
        var parent=new NodeInput("first",null,"章节",null,0,List.of());
        var child=new NodeInput("child","first","子项",BigDecimal.ONE,0,List.of());
        assertThatThrownBy(()->catalog.preview(json.writeValueAsString(doc(parent,child)))).isInstanceOf(BusinessException.class);
        assertThat(course().completedAmount()).isEqualByComparingTo("1");
        assertThatThrownBy(()->archive.archive(date,date.minusDays(1),null,null)).isInstanceOf(BusinessException.class);
    }
}
