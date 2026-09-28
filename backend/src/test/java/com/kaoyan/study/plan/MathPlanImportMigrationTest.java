package com.kaoyan.study.plan;

import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 V12 从空库导入的个人数学一轮计划完整且没有混淆计量单位。 */
@SpringBootTest
@DisplayName("数据迁移：导入数学一轮计划")
class MathPlanImportMigrationTest {

    private static final String GOAL_TITLE = "2027考研数学一轮：张宇基础30讲＋1000题";
    private static final String QUESTION_TASK_TITLE = "张宇《1000题》一轮（按对应章节推进）";

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @Test
    void importsGoalTasksWeeksAndDailyItems() {
        Long goalId = jdbc.queryForObject(
                "SELECT id FROM stage_goal WHERE title = ?", Long.class, GOAL_TITLE);
        assertThat(goalId).isNotNull();

        Integer taskCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task WHERE stage_goal_id = ?", Integer.class, goalId);
        Integer lectureCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM task t JOIN study_unit u ON u.id = t.unit_id
                WHERE t.stage_goal_id = ? AND u.name = '讲'
                """, Integer.class, goalId);
        BigDecimal questionTarget = jdbc.queryForObject("""
                SELECT planned_amount FROM task
                WHERE stage_goal_id = ? AND title = ?
                """, BigDecimal.class, goalId, QUESTION_TASK_TITLE);

        assertThat(taskCount).isEqualTo(30);
        assertThat(lectureCount).isEqualTo(29);
        assertThat(questionTarget).isEqualByComparingTo("1000");

        Integer weekCount = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT wp.week_start_date)
                FROM weekly_plan wp
                JOIN weekly_plan_item item ON item.weekly_plan_id = wp.id
                JOIN task t ON t.id = item.task_id
                WHERE t.stage_goal_id = ?
                """, Integer.class, goalId);
        BigDecimal weeklyQuestionTotal = jdbc.queryForObject("""
                SELECT SUM(item.planned_amount)
                FROM weekly_plan_item item
                JOIN task t ON t.id = item.task_id
                WHERE t.stage_goal_id = ? AND t.title = ?
                """, BigDecimal.class, goalId, QUESTION_TASK_TITLE);

        assertThat(weekCount).isEqualTo(14);
        assertThat(weeklyQuestionTotal).isEqualByComparingTo("1000");

        Integer dailyLectureCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM daily_plan_item item
                JOIN task t ON t.id = item.task_id
                JOIN study_unit u ON u.id = t.unit_id
                WHERE t.stage_goal_id = ? AND u.name = '讲' AND item.source = 'WEEKLY'
                """, Integer.class, goalId);
        Integer dailyQuestionDays = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM daily_plan_item item
                JOIN task t ON t.id = item.task_id
                WHERE t.stage_goal_id = ? AND t.title = ? AND item.source = 'WEEKLY'
                """, Integer.class, goalId, QUESTION_TASK_TITLE);
        BigDecimal dailyQuestionTotal = jdbc.queryForObject("""
                SELECT SUM(item.planned_amount)
                FROM daily_plan_item item
                JOIN task t ON t.id = item.task_id
                WHERE t.stage_goal_id = ? AND t.title = ? AND item.source = 'WEEKLY'
                """, BigDecimal.class, goalId, QUESTION_TASK_TITLE);
        Integer sundayItems = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM daily_plan_item item
                JOIN task t ON t.id = item.task_id
                WHERE t.stage_goal_id = ? AND DAYOFWEEK(item.plan_date) = 1
                """, Integer.class, goalId);

        assertThat(dailyLectureCount).isEqualTo(29);
        assertThat(dailyQuestionDays).isEqualTo(84);
        assertThat(dailyQuestionTotal).isEqualByComparingTo("1000");
        assertThat(sundayItems).as("周日保持机动").isZero();
    }
}
