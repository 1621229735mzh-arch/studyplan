package com.kaoyan.study.plan;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.DailyItemCreateRequest;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.dto.TaskProgressView;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.entity.TaskStatus;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.entity.Subject;
import com.kaoyan.study.settings.entity.StudyUnit;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 核心闭环的业务规则验证：任务 → 每日安排 → 学习记录 → 进度。
 *
 * <p>这些规则对应 PLAN.md 的重点验收项：多天累计、计划不增加完成量、
 * 修改删除后重算、不同单位不混加、重复提交不重复记录、未完成不顺延。
 */
@SpringBootTest
@DisplayName("核心闭环：任务、学习记录与进度")
class PlanLearningFlowIntegrationTest {

    @Autowired
    private SubjectService subjectService;
    @Autowired
    private StudyUnitService studyUnitService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private DailyPlanService dailyPlanService;
    @Autowired
    private StudyRecordService studyRecordService;
    @Autowired
    private JdbcTemplate jdbc;

    private Subject subject;
    private StudyUnit unit;
    private StudyUnit otherUnit;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @BeforeEach
    void setUp() {
        // 每次用唯一名称，保证在同一个外部测试库上可以重复运行
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        subject = subjectService.create(new SubjectRequest("测试科目-" + suffix, "专业课", null, 0, true));
        unit = studyUnitService.create(new StudyUnitRequest("讲-" + suffix, 0, true));
        otherUnit = studyUnitService.create(new StudyUnitRequest("页-" + suffix, 1, true));
    }

    @Test
    @DisplayName("创建计划不增加完成量，学习记录才决定进度")
    void planDoesNotIncreaseCompletion() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "操作系统第 1-3 讲",
                unit.getId(), new BigDecimal("3")));

        TaskProgressView before = taskService.progress(task.getId());
        assertThat(before.completedAmount()).isEqualByComparingTo("0");
        assertThat(before.remainingAmount()).isEqualByComparingTo("3");

        // 排入今日安排仍然不产生完成量
        dailyPlanService.addItem(LocalDate.now(), new DailyItemCreateRequest(task.getId(),
                new BigDecimal("2"), null));

        TaskProgressView afterPlanning = taskService.progress(task.getId());
        assertThat(afterPlanning.completedAmount()).as("计划不是完成量").isEqualByComparingTo("0");
        assertThat(afterPlanning.remainingAmount()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("同一任务分多天完成时累计数量正确，任务级剩余量同步变化")
    void accumulatesAcrossDays() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "数据结构第 1-3 讲",
                unit.getId(), new BigDecimal("3")));

        record(task, LocalDate.now().minusDays(1), "2", null);
        TaskProgressView afterFirstDay = taskService.progress(task.getId());
        assertThat(afterFirstDay.completedAmount()).isEqualByComparingTo("2");
        assertThat(afterFirstDay.remainingAmount()).isEqualByComparingTo("1");

        record(task, LocalDate.now(), "1", null);
        TaskProgressView afterSecondDay = taskService.progress(task.getId());
        assertThat(afterSecondDay.completedAmount()).isEqualByComparingTo("3");
        assertThat(afterSecondDay.remainingAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("修改学习记录后进度重新计算")
    void progressRecalculatesAfterUpdate() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "组成原理第 1 章",
                unit.getId(), new BigDecimal("5")));
        StudyRecordView created = record(task, LocalDate.now(), "2", null);

        studyRecordService.update(created.id(), new com.kaoyan.study.learning.dto.StudyRecordUpdateRequest(
                task.getId(), null, null, LocalDate.now(), new BigDecimal("4"), 90, "补记", null, created.version()));

        assertThat(taskService.progress(task.getId()).completedAmount()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("删除学习记录后进度重新计算")
    void progressRecalculatesAfterDelete() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "计算机网络第 1 章",
                unit.getId(), new BigDecimal("4")));
        StudyRecordView created = record(task, LocalDate.now(), "3", null);

        studyRecordService.delete(created.id(), created.version());

        assertThat(taskService.progress(task.getId()).completedAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("重复提交同一 clientToken 不会生成第二条记录")
    void duplicateSubmissionIsIdempotent() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "错题整理",
                unit.getId(), new BigDecimal("10")));
        String token = UUID.randomUUID().toString();

        StudyRecordView first = record(task, LocalDate.now(), "2", token);
        StudyRecordView second = record(task, LocalDate.now(), "2", token);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(taskService.progress(task.getId()).completedAmount())
                .as("重发不应重复计数").isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("计量单位与任务不一致的记录被拒绝（不同单位不混加）")
    void unitMustMatchTask() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "操作系统第 4 讲",
                unit.getId(), new BigDecimal("1")));

        assertThatThrownBy(() -> studyRecordService.create(new StudyRecordCreateRequest(
                task.getId(), null, otherUnit.getId(), LocalDate.now(), new BigDecimal("5"), 60, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("计量单位");
    }

    @Test
    @DisplayName("计划外记录填写完成量时必须给出科目与单位")
    void adHocRecordWithAmountRequiresSubjectAndUnit() {
        assertThatThrownBy(() -> studyRecordService.create(new StudyRecordCreateRequest(
                null, null, null, LocalDate.now(), new BigDecimal("1"), 30, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("计划外");
    }

    @Test
    @DisplayName("只记时长的记录不增加完成量，但计入学习时长")
    void timeOnlyRecordCountsDurationOnly() {
        StudyRecordView record = studyRecordService.create(new StudyRecordCreateRequest(
                null, null, null, LocalDate.now(), null, 35, "听网课", "只记时长", null));

        assertThat(record.amount()).as("没有完成量").isNull();
        assertThat(record.unitId()).as("不需要单位").isNull();
        assertThat(record.durationMinutes()).isEqualTo(35);
    }

    @Test
    @DisplayName("有学习记录的任务不能删除，只能归档")
    void taskWithRecordsCannotBeDeleted() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "真题套卷",
                unit.getId(), new BigDecimal("2")));
        record(task, LocalDate.now(), "1", null);

        assertThatThrownBy(() -> taskService.delete(task.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("学习记录");

        // 归档是允许的处置方式
        Task archived = taskService.update(task.getId(), new TaskUpdateRequest(subject.getId(), null,
                task.getTitle(), unit.getId(), task.getPlannedAmount(), TaskStatus.ARCHIVED, task.getVersion()));
        assertThat(archived.getStatus()).isEqualTo(TaskStatus.ARCHIVED);
    }

    @Test
    @DisplayName("未完成的安排不会自动出现在次日")
    void unfinishedItemsAreNotCarriedOver() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "第三章练习",
                unit.getId(), new BigDecimal("20")));
        LocalDate today = LocalDate.now();
        dailyPlanService.addItem(today, new DailyItemCreateRequest(task.getId(), new BigDecimal("10"), null));

        assertThat(dailyPlanService.list(today))
                .extracting(DailyPlanItemView::taskId)
                .contains(task.getId());
        assertThat(dailyPlanService.list(today.plusDays(1)))
                .as("次日不应自动出现昨天的未完成安排")
                .extracting(DailyPlanItemView::taskId)
                .doesNotContain(task.getId());
    }

    private StudyRecordView record(Task task, LocalDate date, String amount, String clientToken) {
        return studyRecordService.create(new StudyRecordCreateRequest(task.getId(), null, null, date,
                new BigDecimal(amount), 60, "学习内容", "复盘", clientToken));
    }

    @Test
    void recordedTaskCannotChangeUnitOrSubjectEvenAfterSoftDelete() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "历史单位",
                unit.getId(), BigDecimal.TEN));
        StudyRecordView record = record(task, LocalDate.now(), "2", null);
        Subject otherSubject = subjectService.create(new SubjectRequest("新科目-" + UUID.randomUUID(), null, null, 0, true));

        assertThatThrownBy(() -> taskService.update(task.getId(), new TaskUpdateRequest(subject.getId(), null,
                task.getTitle(), otherUnit.getId(), BigDecimal.TEN, task.getStatus(), task.getVersion())))
                .isInstanceOf(ConflictException.class).hasMessageContaining("科目或计量单位");
        assertThatThrownBy(() -> taskService.update(task.getId(), new TaskUpdateRequest(otherSubject.getId(), null,
                task.getTitle(), unit.getId(), BigDecimal.TEN, task.getStatus(), task.getVersion())))
                .isInstanceOf(ConflictException.class);
        assertThat(taskService.progress(task.getId()).completedAmount()).isEqualByComparingTo("2");
        assertThat(taskService.progress(task.getId()).unitId()).isEqualTo(unit.getId());

        studyRecordService.delete(record.id(), record.version());
        assertThatThrownBy(() -> taskService.delete(task.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("学习记录");
        assertThatThrownBy(() -> taskService.update(task.getId(), new TaskUpdateRequest(subject.getId(), null,
                task.getTitle(), otherUnit.getId(), BigDecimal.TEN, task.getStatus(), task.getVersion())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unusedTaskCanStillChangeUnit() {
        Task task = taskService.create(new TaskCreateRequest(subject.getId(), null, "尚未学习",
                unit.getId(), BigDecimal.ONE));
        Task updated = taskService.update(task.getId(), new TaskUpdateRequest(subject.getId(), null,
                task.getTitle(), otherUnit.getId(), BigDecimal.TEN, task.getStatus(), task.getVersion()));
        assertThat(updated.getUnitId()).isEqualTo(otherUnit.getId());
    }

    @Test
    void softDeletedAdHocRecordStillProtectsUnitAndSubject() {
        StudyRecordView record = studyRecordService.create(new StudyRecordCreateRequest(null, subject.getId(),
                unit.getId(), LocalDate.now(), BigDecimal.ONE, 20, "计划外", null, null));
        studyRecordService.delete(record.id(), record.version());
        assertThatThrownBy(() -> studyUnitService.delete(unit.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> subjectService.delete(subject.getId())).isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"stage_goal", "review_item", "memo"})
    void subjectWithoutTasksStillChecksOtherReferences(String table) {
        // 独立构造每一种外键引用，确保不是任务/学习记录的检查偶然挡住了删除。
        switch (table) {
            case "stage_goal" -> jdbc.update("INSERT INTO stage_goal (subject_id, title) VALUES (?, '阶段目标')", subject.getId());
            case "review_item" -> jdbc.update("INSERT INTO review_item (subject_id, title) VALUES (?, '已有复习项')", subject.getId());
            case "memo" -> jdbc.update("INSERT INTO memo (subject_id, content) VALUES (?, '笔记')", subject.getId());
            default -> throw new IllegalArgumentException(table);
        }
        assertThatThrownBy(() -> subjectService.delete(subject.getId())).isInstanceOf(ConflictException.class);
        assertThat(subjectService.get(subject.getId())).isNotNull();
    }
}
