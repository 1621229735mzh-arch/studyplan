package com.kaoyan.study.review;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.review.dto.ReviewConfirmRequest;
import com.kaoyan.study.review.dto.ReviewConfirmResponse;
import com.kaoyan.study.review.dto.ReviewItemCreateRequest;
import com.kaoyan.study.review.dto.ReviewRecordCreateRequest;
import com.kaoyan.study.review.dto.ReviewRecordResponse;
import com.kaoyan.study.review.dto.ReviewSuggestionResponse;
import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewItem;
import com.kaoyan.study.review.entity.ReviewStatus;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.dto.StudySettingsRequest;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.entity.StudyUnit;
import com.kaoyan.study.settings.entity.Subject;
import com.kaoyan.study.settings.service.StudySettingsService;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 复习机制的规则验证：只复习已学内容、建议需确认、额度约束、间隔来自设置。
 *
 * <p>每日复习额度是按日期累计的，所以每个用例使用各自独立的日期，
 * 避免同一测试库中其它用例已安排的复习影响额度判断。
 */
@SpringBootTest
@DisplayName("复习：加入、反馈、额度与确认安排")
class ReviewFlowIntegrationTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private StudyRecordService studyRecordService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private StudyUnitService studyUnitService;
    @Autowired
    private StudySettingsService studySettingsService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Subject subject;
    private StudyUnit unit;
    /** 每个用例独立的安排日期。 */
    private LocalDate targetDate;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        subject = subjectService.create(new SubjectRequest("复习科目-" + suffix, "专业课", null, 0, true));
        unit = studyUnitService.create(new StudyUnitRequest("讲-" + suffix, 0, true));
        targetDate = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(365, 3_650));
    }

    @Test
    @DisplayName("还没学过的内容不能加入复习")
    void onlyStudiedContentCanBeReviewed() {
        Task task = newTask("尚未学习的任务");

        assertThatThrownBy(() -> reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经学过");
    }

    @Test
    @DisplayName("复习反馈不增加首次学习量（复习记录与学习记录分开）")
    void reviewDoesNotAddToFirstTimeStudyAmount() {
        Task task = newTask("操作系统第 1 讲");
        studyOne(task, "1");
        long recordsBefore = studyRecordService.list(null, null, task.getId(), null).size();

        ReviewItem item = reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 15));
        reviewService.recordReview(new ReviewRecordCreateRequest(item.getId(), targetDate,
                MasteryResult.MASTERED, 10, "第一轮复习", null, null));

        assertThat(studyRecordService.list(null, null, task.getId(), null))
                .as("复习不应写入学习记录，否则完成量会被重复计算")
                .hasSize((int) recordsBefore);
    }

    @Test
    @DisplayName("未设置复习额度时确认安排会被拒绝，建议里也明确提示")
    void confirmRequiresConfiguredQuota() {
        Task task = newTask("组成原理第 1 章");
        studyOne(task, "1");
        ReviewItem item = reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 20));

        setReviewQuota(null);
        ReviewSuggestionResponse suggestions = reviewService.suggestions(LocalDate.now());
        assertThat(suggestions.dailyReviewMinutes()).isNull();
        assertThat(suggestions.remainingMinutes()).isNull();
        assertThat(suggestions.warnings()).anyMatch(warning -> warning.contains("复习额度"));

        assertThatThrownBy(() -> reviewService.confirm(new ReviewConfirmRequest(targetDate,
                List.of(new ReviewConfirmRequest.Item(item.getId(), null)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("复习额度");
    }

    @Test
    @DisplayName("超出额度的内容留在待安排列表并给出原因")
    void overQuotaItemsStayPending() {
        Task first = newTask("章节 A");
        Task second = newTask("章节 B");
        studyOne(first, "1");
        studyOne(second, "1");
        ReviewItem itemA = reviewService.addToReview(new ReviewItemCreateRequest(first.getId(), null, null, 30));
        ReviewItem itemB = reviewService.addToReview(new ReviewItemCreateRequest(second.getId(), null, null, 40));

        setReviewQuota(45);
        ReviewConfirmResponse response = reviewService.confirm(new ReviewConfirmRequest(targetDate,
                List.of(new ReviewConfirmRequest.Item(itemA.getId(), null),
                        new ReviewConfirmRequest.Item(itemB.getId(), null))));

        assertThat(response.scheduled()).extracting(r -> r.id()).contains(itemA.getId());
        assertThat(response.rejected()).singleElement()
                .satisfies(rejected -> {
                    assertThat(rejected.reviewItemId()).isEqualTo(itemB.getId());
                    assertThat(rejected.reason()).contains("额度");
                });
        assertThat(reviewService.get(itemB.getId()).getStatus())
                .as("超额内容必须留在待安排列表")
                .isEqualTo(ReviewStatus.PENDING);
        assertThat(reviewService.get(itemB.getId()).getNextReviewDate())
                .as("待安排内容保留到期信息")
                .isNotNull();
    }

    @Test
    @DisplayName("缺少预计用时时要求补充，而不是按 0 计入额度")
    void missingEstimateMustBeSupplied() {
        Task task = newTask("计算机网络第 2 章");
        studyOne(task, "1");
        ReviewItem item = reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, null));
        setReviewQuota(60);

        ReviewConfirmResponse response = reviewService.confirm(new ReviewConfirmRequest(targetDate,
                List.of(new ReviewConfirmRequest.Item(item.getId(), null))));
        assertThat(response.rejected()).singleElement()
                .satisfies(rejected -> assertThat(rejected.reason()).contains("预计用时"));

        ReviewConfirmResponse supplied = reviewService.confirm(new ReviewConfirmRequest(targetDate,
                List.of(new ReviewConfirmRequest.Item(item.getId(), 25))));
        assertThat(supplied.scheduled()).hasSize(1);
    }

    @Test
    @DisplayName("掌握反馈按设置的间隔推导下次日期，未配置间隔时不给具体日期")
    void nextDateFollowsConfiguredInterval() {
        Task task = newTask("数据结构第 5 讲");
        studyOne(task, "1");
        ReviewItem item = reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 10));

        setIntervals(null, null, null);
        ReviewRecordResponse withoutInterval = reviewService.recordReview(new ReviewRecordCreateRequest(
                item.getId(), targetDate, MasteryResult.VAGUE, 12, null, null, null));
        assertThat(withoutInterval.nextReviewDate()).isNull();
        assertThat(withoutInterval.intervalConfigured()).isFalse();

        setIntervals(1, 3, 7);
        ReviewRecordResponse withInterval = reviewService.recordReview(new ReviewRecordCreateRequest(
                item.getId(), targetDate, MasteryResult.MASTERED, 12, null, null, null));
        assertThat(withInterval.nextReviewDate()).isEqualTo(targetDate.plusDays(7));
        assertThat(withInterval.intervalConfigured()).isTrue();
    }

    @Test
    @DisplayName("重复提交同一复习反馈不会生成第二条记录")
    void duplicateReviewSubmissionIsIdempotent() {
        Task task = newTask("真题选择题");
        studyOne(task, "1");
        ReviewItem item = reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 10));
        String token = UUID.randomUUID().toString();

        ReviewRecordResponse first = reviewService.recordReview(new ReviewRecordCreateRequest(
                item.getId(), targetDate, MasteryResult.FORGOT, 20, null, null, token));
        ReviewRecordResponse second = reviewService.recordReview(new ReviewRecordCreateRequest(
                item.getId(), targetDate, MasteryResult.FORGOT, 20, null, null, token));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(reviewService.records(item.getId())).hasSize(1);
    }

    private Task newTask(String title) {
        return taskService.create(new TaskCreateRequest(subject.getId(), null, title, unit.getId(),
                new BigDecimal("5")));
    }

    @Test
    void adHocReviewRequiresARealNonDeletedSourceWithMatchingSubject() {
        assertThatThrownBy(() -> reviewService.addToReview(
                new ReviewItemCreateRequest(null, subject.getId(), "未学内容", 10)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("学习记录");

        StudyRecordView source = studyRecordService.create(new StudyRecordCreateRequest(null, subject.getId(),
                unit.getId(), LocalDate.now(), BigDecimal.ONE, 15, "已学内容", null, null));
        assertThatThrownBy(() -> reviewService.addToReview(
                new ReviewItemCreateRequest(null, -1L, "错误科目", 10, source.id())))
                .isInstanceOf(BusinessException.class).hasMessageContaining("科目");
        ReviewItem item = reviewService.addToReview(
                new ReviewItemCreateRequest(null, null, "已学内容", 10, source.id()));
        assertThat(item.getSourceRecordId()).isEqualTo(source.id());
        assertThat(item.getSubjectId()).isEqualTo(subject.getId());

        studyRecordService.delete(source.id(), source.version());
        assertThatThrownBy(() -> reviewService.addToReview(
                new ReviewItemCreateRequest(null, null, "已删除来源", 10, source.id())))
                .isInstanceOf(com.kaoyan.study.common.exception.NotFoundException.class);
    }

    @Test
    void futureStudyDoesNotProveContentAlreadyLearned() {
        Task task = newTask("未来补录");
        studyRecordService.create(new StudyRecordCreateRequest(task.getId(), null, null,
                LocalDate.now().plusDays(1), BigDecimal.ONE, 30, null, null, null));
        assertThatThrownBy(() -> reviewService.addToReview(new ReviewItemCreateRequest(task.getId(), null, null, 10)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经学过");
    }

    @Test
    void concurrentConfirmationsShareQuotaEvenWithEarlierTransactionSnapshots() throws Exception {
        Task first = newTask("并发 A");
        Task second = newTask("并发 B");
        studyOne(first, "1");
        studyOne(second, "1");
        ReviewItem a = reviewService.addToReview(new ReviewItemCreateRequest(first.getId(), null, null, 30));
        ReviewItem b = reviewService.addToReview(new ReviewItemCreateRequest(second.getId(), null, null, 30));
        setReviewQuota(45);
        CyclicBarrier ready = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(a, b).stream().map(item -> executor.submit(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        // 故意让两个事务都先看到“零占用”；后续确认必须使用当前读。
                        assertThat(reviewService.suggestions(targetDate).scheduledMinutes()).isZero();
                        try {
                            ready.await(10, TimeUnit.SECONDS);
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                        return reviewService.confirm(new ReviewConfirmRequest(targetDate,
                                List.of(new ReviewConfirmRequest.Item(item.getId(), null))));
                    }))).toList();
            ReviewConfirmResponse one = futures.get(0).get(20, TimeUnit.SECONDS);
            ReviewConfirmResponse two = futures.get(1).get(20, TimeUnit.SECONDS);
            assertThat(one.scheduled().size() + two.scheduled().size()).isEqualTo(1);
            assertThat(one.rejected().size() + two.rejected().size()).isEqualTo(1);
        }
        assertThat(reviewService.scheduledOn(targetDate)).hasSize(1);
        assertThat(reviewService.suggestions(targetDate).scheduledMinutes()).isEqualTo(30);
    }

    private void studyOne(Task task, String amount) {
        studyRecordService.create(new StudyRecordCreateRequest(task.getId(), null, null, LocalDate.now(),
                new BigDecimal(amount), 60, "学习内容", null, null));
    }

    /** 显式设置复习额度；传 null 表示清空（未设置），用于验证缺额度时的行为。 */
    private void setReviewQuota(Integer minutes) {
        StudySettings current = studySettingsService.get();
        studySettingsService.update(new StudySettingsRequest(current.getExamDate(),
                current.getDailyStudyMinutes(), minutes, current.getReviewPhase(),
                current.getReviewIntervalForgotDays(), current.getReviewIntervalVagueDays(),
                current.getReviewIntervalMasteredDays(), current.getVersion()));
    }

    private void setIntervals(Integer forgot, Integer vague, Integer mastered) {
        StudySettings current = studySettingsService.get();
        studySettingsService.update(new StudySettingsRequest(current.getExamDate(),
                current.getDailyStudyMinutes(), current.getDailyReviewMinutes(), current.getReviewPhase(),
                forgot, vague, mastered, current.getVersion()));
    }
}
