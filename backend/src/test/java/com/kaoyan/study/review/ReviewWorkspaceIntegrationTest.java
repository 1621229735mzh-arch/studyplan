package com.kaoyan.study.review;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.service.RecordArchiveService;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.progress.service.CatalogService;
import com.kaoyan.study.review.dto.*;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.service.*;
import com.kaoyan.study.settings.dto.*;
import com.kaoyan.study.settings.service.*;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ReviewWorkspaceIntegrationTest {
    @Autowired ReviewWorkspaceService workspace;
    @Autowired ReviewService reviews;
    @Autowired StudyRecordService records;
    @Autowired TaskService tasks;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired RecordArchiveService archive;
    @Autowired CatalogService catalog;
    @Autowired StudyDay studyDay;
    private Long subjectId;
    private Long taskId;
    private ReviewItem item;
    private LocalDate date;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "review-test");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }
    @BeforeEach void setup() {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        subjectId = subjects.create(new SubjectRequest("复习框架-" + suffix, "专业课", null, 0, true)).getId();
        var unit = units.create(new StudyUnitRequest("讲-" + suffix, 0, true));
        var task = tasks.create(new TaskCreateRequest(subjectId, null, "已学测试内容", unit.getId(), BigDecimal.TEN));
        taskId = task.getId(); date = studyDay.today().minusDays(1);
        records.create(new StudyRecordCreateRequest(taskId, null, null, date.minusDays(10), BigDecimal.ONE, null, "首次学习", null, UUID.randomUUID().toString()));
        item = reviews.addToReview(new ReviewItemCreateRequest(taskId, null, null, null));
    }

    @Test void unconfirmedContentDoesNotCreateCalendarEntries() {
        var view = workspace.workspace(subjectId);
        assertThat(view.entries()).isEmpty(); assertThat(view.months()).isEmpty();
        assertThat(view.totals().itemCount()).isEqualTo(1);
        assertThat(view.totals().completedReviews()).isZero();
        assertThat(reviews.dayTasks(date)).noneMatch(r -> r.id().equals(item.getId()));
    }

    @Test void scheduleIsVersionedAndTodayReadsTheSameItem() {
        var scheduled = workspace.schedule(new ReviewScheduleRequest(item.getId(), date, item.getVersion()));
        assertThat(reviews.dayTasks(date)).anyMatch(r -> r.id().equals(item.getId()) && r.actionable());
        assertThat(workspace.workspace(subjectId).entries()).singleElement().satisfies(e -> {
            assertThat(e.date()).isEqualTo(date); assertThat(e.completionPercent()).isZero(); assertThat(e.scheduled()).isTrue();
        });
        assertThatThrownBy(() -> workspace.schedule(new ReviewScheduleRequest(item.getId(), date.plusDays(1), item.getVersion())))
                .isInstanceOf(ConflictException.class);
        workspace.schedule(new ReviewScheduleRequest(item.getId(), null, scheduled.version()));
        assertThat(workspace.workspace(subjectId).entries()).isEmpty();
    }

    @Test void partialReviewIsVisibleInArchiveWithoutIncreasingFirstStudyOrCompletedSessions() {
        var scheduled = workspace.schedule(new ReviewScheduleRequest(item.getId(), date, item.getVersion()));
        reviews.recordDayProgress(new ReviewDayProgressRequest(item.getId(), date, 40, null, scheduled.version(), null, UUID.randomUUID().toString()));
        var view = workspace.workspace(subjectId);
        assertThat(view.entries()).singleElement().satisfies(e -> assertThat(e.completionPercent()).isEqualByComparingTo("40"));
        assertThat(view.totals().completedReviews()).isZero();
        var tree = archive.archive(date, date, subjectId, taskId);
        assertThat(tree.stats().recordCount()).isZero(); assertThat(tree.stats().reviewRecordCount()).isEqualTo(1);
        assertThat(tree.stats().amounts()).isEmpty();
        assertThat(tree.months().getFirst().weeks().getFirst().days().getFirst().reviews()).hasSize(1);
        assertThat(tree.months().getFirst().weeks().getFirst().days().getFirst().groups()).isEmpty();
        var current = reviews.get(item.getId());
        workspace.schedule(new ReviewScheduleRequest(item.getId(), null, current.getVersion()));
        assertThat(workspace.workspace(subjectId).entries()).singleElement().satisfies(e -> assertThat(e.scheduled()).isFalse());
        assertThat(archive.archive(date, date, subjectId, taskId).stats().reviewRecordCount()).isEqualTo(1);
    }

    @Test void completedHistoryRemainsAcrossNextScheduleAndProgressIsDistinctByContent() {
        reviews.recordReview(new ReviewRecordCreateRequest(item.getId(), date, MasteryResult.VAGUE, null, "测试反馈", null, UUID.randomUUID().toString()));
        var current = reviews.get(item.getId());
        workspace.schedule(new ReviewScheduleRequest(item.getId(), date.plusDays(3), current.getVersion()));
        var view = workspace.workspace(subjectId);
        assertThat(view.entries()).hasSize(2);
        assertThat(view.totals().reviewedItems()).isEqualTo(1); assertThat(view.totals().completedReviews()).isEqualTo(1);
        assertThat(catalog.library().reviews()).filteredOn(r -> r.subjectId().equals(subjectId)).singleElement()
                .satisfies(r -> assertThat(r.totals()).isEqualTo(view.totals()));
        assertThat(records.list(null, null, taskId, subjectId)).hasSize(1);
        assertThatThrownBy(() -> workspace.schedule(new ReviewScheduleRequest(item.getId(), date, reviews.get(item.getId()).getVersion())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("当天已完成");
    }

    @Test void subjectAndTaskFiltersApplyToBothStudyAndReviewAndArchivedCannotBeScheduled() {
        reviews.recordReview(new ReviewRecordCreateRequest(item.getId(), date, MasteryResult.MASTERED, null, null, null, UUID.randomUUID().toString()));
        assertThat(archive.archive(date, date, -1L, null).months()).isEmpty();
        assertThat(archive.archive(date, date, null, -1L).months()).isEmpty();
        reviews.archive(item.getId(), reviews.get(item.getId()).getVersion());
        assertThatThrownBy(() -> workspace.schedule(new ReviewScheduleRequest(item.getId(), date.plusDays(1), reviews.get(item.getId()).getVersion())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("归档");
        assertThat(archive.archive(date, date, subjectId, taskId).stats().reviewRecordCount()).isEqualTo(1);
    }

    @Test void reviewOnlyDatesAcrossMonthBoundaryAreNotDuplicatedAndRepeatedContentCountsOnce() {
        var last = studyDay.today().withDayOfMonth(1).minusDays(1);
        for (var day : java.util.List.of(last, last.plusDays(1)))
            reviews.recordReview(new ReviewRecordCreateRequest(item.getId(), day, MasteryResult.VAGUE, null, null, null, UUID.randomUUID().toString()));
        var tree = archive.archive(last, last.plusDays(1), subjectId, taskId);
        assertThat(tree.months()).hasSize(2);
        assertThat(tree.stats().reviewRecordCount()).isEqualTo(2);
        assertThat(tree.months().stream().mapToInt(m -> m.stats().reviewRecordCount()).sum()).isEqualTo(2);
        assertThat(workspace.workspace(subjectId).totals().reviewedItems()).isEqualTo(1);
        assertThat(workspace.workspace(subjectId).totals().completedReviews()).isEqualTo(2);
    }
}
