package com.kaoyan.study.progress;

import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordUpdateRequest;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.DailyItemCreateRequest;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.entity.PlanSource;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.progress.dto.UnitProgressView;
import com.kaoyan.study.progress.service.ProgressService;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProgressWorkspaceIntegrationTest {
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService plans;
    @Autowired StudyRecordService records;
    @Autowired ProgressService progress;
    @Autowired JdbcTemplate jdbc;
    Long subjectId;
    Long unitId;
    final LocalDate date = LocalDate.of(2031, 4, 7);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @BeforeEach
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        subjectId = subjects.create(new SubjectRequest("进度-" + suffix, "专业课", null, 0, true)).getId();
        unitId = units.create(new StudyUnitRequest("讲-" + suffix, 0, true)).getId();
    }

    Task task() { return tasks.create(new TaskCreateRequest(subjectId, null, "测试学习", unitId, new BigDecimal("100"))); }
    UnitProgressView row() { return progress.overview(date, date.plusDays(1)).items().stream().filter(r -> r.subjectId().equals(subjectId) && r.unitId().equals(unitId)).findFirst().orElseThrow(); }

    @Test
    void comparesDailyPlansInSameRangeWithoutMultiplyingRecords() {
        Task task = task();
        task(); // 未排期的任务总量不应计入
        plans.addItem(date, new DailyItemCreateRequest(task.getId(), new BigDecimal("2"), PlanSource.MANUAL));
        jdbc.update("INSERT INTO daily_plan_item(plan_date, task_id, planned_amount, source) VALUES (?, ?, 3, 'IMPORT')", date, task.getId());
        plans.addItem(date.plusDays(1), new DailyItemCreateRequest(task.getId(), new BigDecimal("4"), null));
        plans.addItem(date.minusDays(1), new DailyItemCreateRequest(task.getId(), new BigDecimal("80"), null));
        jdbc.update("INSERT INTO daily_plan_item(plan_date, task_id, planned_amount, source) VALUES (?, ?, 50, 'REVIEW')", date, task.getId());
        records.create(new StudyRecordCreateRequest(task.getId(), null, null, date, new BigDecimal("1.123456"), 30, null, null, UUID.randomUUID().toString()));
        records.create(new StudyRecordCreateRequest(task.getId(), null, null, date.minusDays(1), new BigDecimal("10"), 90, null, null, UUID.randomUUID().toString()));
        assertThat(row().plannedAmount()).isEqualByComparingTo("9");
        assertThat(row().completedAmount()).isEqualByComparingTo("1.123456");
        assertThat(row().studyMinutes()).isEqualTo(30);
    }

    @Test
    void recordWithoutTaskKeepsMissingPlanAndDurationUnknown() {
        records.create(new StudyRecordCreateRequest(null, subjectId, unitId, date, new BigDecimal("2"), null, "独立学习", null, UUID.randomUUID().toString()));
        assertThat(row().plannedAmount()).isNull();
        assertThat(row().completedAmount()).isEqualByComparingTo("2");
        assertThat(row().studyMinutes()).isNull();
    }

    @Test
    void editingAndDeletingRecordsUpdatesProgressAndKeepsUnitsSeparate() {
        Task task = task();
        plans.addItem(date, new DailyItemCreateRequest(task.getId(), new BigDecimal("8"), null));
        var record = records.create(new StudyRecordCreateRequest(task.getId(), null, null, date, new BigDecimal("2"), 20, null, null, UUID.randomUUID().toString()));
        Long otherUnit = units.create(new StudyUnitRequest("页-" + UUID.randomUUID().toString().substring(0,8), 0, true)).getId();
        records.create(new StudyRecordCreateRequest(null, subjectId, otherUnit, date, new BigDecimal("50"), 40, null, null, UUID.randomUUID().toString()));
        var updated = records.update(record.id(), new StudyRecordUpdateRequest(task.getId(), null, null, date, new BigDecimal("3"), 25, "修改内容", null, record.version()));
        assertThat(row().completedAmount()).isEqualByComparingTo("3");
        assertThat(row().studyMinutes()).isEqualTo(25);
        records.delete(updated.id(), updated.version());
        assertThat(row().completedAmount()).isEqualByComparingTo("0");
        assertThat(row().plannedAmount()).isEqualByComparingTo("8");
        assertThat(row().studyMinutes()).isNull();
    }

    @Test
    void removingArrangementPreservesHistoryAndArchivedTasksKeepExistingSchedules() {
        Task task = task();
        plans.addItem(date, new DailyItemCreateRequest(task.getId(), new BigDecimal("6"), null));
        records.create(new StudyRecordCreateRequest(task.getId(), null, null, date, new BigDecimal("2"), 30, null, null, UUID.randomUUID().toString()));
        jdbc.update("UPDATE task SET status = 'ARCHIVED' WHERE id = ?", task.getId());
        assertThat(row().plannedAmount()).isEqualByComparingTo("6");
        var arrangement = plans.list(date).stream().filter(item -> item.taskId().equals(task.getId())).findFirst().orElseThrow();
        plans.removeItem(date, arrangement.id(), arrangement.version());
        assertThat(row().plannedAmount()).isNull();
        assertThat(row().completedAmount()).isEqualByComparingTo("2");
    }

    @Test
    void emptyTasksDoNotPreventTimeOnlyTrend() {
        records.create(new StudyRecordCreateRequest(null, null, null, date, null, 35, "自主学习", null, UUID.randomUUID().toString()));
        assertThat(progress.overview(date, date).items()).noneMatch(r -> r.subjectId().equals(subjectId));
        assertThat(progress.trend(date, date)).anySatisfy(point -> {
            assertThat(point.date()).isEqualTo(date);
            assertThat(point.studyMinutes()).isGreaterThanOrEqualTo(35);
        });
    }
}
