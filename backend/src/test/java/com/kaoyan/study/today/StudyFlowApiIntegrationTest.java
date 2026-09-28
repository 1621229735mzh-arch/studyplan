package com.kaoyan.study.today;

import com.kaoyan.study.support.HttpTestClient;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端接口验证：登录 → 配置 → 计划 → 记录 → 今日 → 进度 → 复习建议。
 *
 * <p>走真实 HTTP 与真实 MySQL，用来确认控制器、DTO 序列化、安全过滤链与数据库链路
 * 在完整流程下协同正常（单元测试无法覆盖这一层）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("端到端：从配置到今日与进度")
class StudyFlowApiIntegrationTest {

    @LocalServerPort
    private int port;

    private HttpTestClient client;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @BeforeEach
    void setUp() {
        client = new HttpTestClient("http://127.0.0.1:" + port);
        String token = client.fetchCsrfToken();
        HttpTestClient.Response login = client.post("/api/auth/login",
                """
                {"username":"kaoyan","password":"test-only-password"}""", token);
        assertThat(login.status()).as("登录应成功，测试其余接口前先建立会话").isEqualTo(200);
    }

    @Test
    @DisplayName("完整闭环：科目/单位 → 任务 → 今日安排 → 学习记录 → 今日与进度")
    void fullFlowWorksOverHttp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        long subjectId = createId("/api/settings/subjects",
                """
                {"name":"接口科目-%s","category":"专业课","sortOrder":0,"enabled":true}""".formatted(suffix));
        long unitId = createId("/api/settings/units",
                """
                {"name":"接口单位-%s","sortOrder":0,"enabled":true}""".formatted(suffix));
        long taskId = createId("/api/plan/tasks",
                """
                {"subjectId":%d,"title":"接口任务-%s","unitId":%d,"plannedAmount":3}"""
                        .formatted(subjectId, suffix, unitId));

        String today = LocalDate.now().toString();

        // 今日安排
        HttpTestClient.Response planned = client.post("/api/plan/days/" + today + "/items",
                """
                {"taskId":%d,"plannedAmount":2}""".formatted(taskId), csrf());
        assertThat(planned.status()).isEqualTo(200);
        assertThat(planned.body()).contains("接口任务-" + suffix);
        assertThat(planned.body()).contains("\"completedAmount\":0");

        // 提交学习记录
        HttpTestClient.Response record = client.post("/api/learning/records",
                """
                {"taskId":%d,"recordDate":"%s","amount":2,"durationMinutes":90,"content":"第 1-2 讲","clientToken":"%s"}"""
                        .formatted(taskId, today, UUID.randomUUID()), csrf());
        assertThat(record.status()).isEqualTo(201);
        assertThat(record.string("$.unitName")).isEqualTo("接口单位-" + suffix);

        // 今日视图：包含今天提交的记录，时长合计至少包含本次的 90 分钟
        // （同一测试库中其它用例也会在今天产生记录，因此只做下界断言）
        HttpTestClient.Response todayView = client.get("/api/today?date=" + today);
        assertThat(todayView.status()).isEqualTo(200);
        assertThat(todayView.string("$.date")).isEqualTo(today);
        assertThat(todayView.number("$.studyMinutes").intValue()).isGreaterThanOrEqualTo(90);
        assertThat(todayView.body()).contains("接口任务-" + suffix);
        assertThat(todayView.body()).contains("第 1-2 讲");

        // 任务级进度：计划 3、完成 2、剩余 1
        HttpTestClient.Response progress = client.get("/api/plan/tasks/" + taskId + "/progress");
        assertThat(progress.status()).isEqualTo(200);
        assertThat(progress.number("$.plannedAmount").doubleValue()).isEqualTo(3.0);
        assertThat(progress.number("$.completedAmount").doubleValue()).isEqualTo(2.0);
        assertThat(progress.number("$.remainingAmount").doubleValue()).isEqualTo(1.0);

        // 进度概览按科目+单位返回，不混加单位
        HttpTestClient.Response overview = client.get("/api/progress/overview");
        assertThat(overview.status()).isEqualTo(200);
        assertThat(overview.body()).contains("接口单位-" + suffix);

        // 复习建议：未设置复习额度时必须明确提示，而不是宣称符合额度。
        // 复习额度是按用户保存的，这里先显式清空，避免受其他用例影响。
        clearReviewQuota();
        HttpTestClient.Response suggestions = client.get("/api/review/suggestions?date=" + today);
        assertThat(suggestions.status()).isEqualTo(200);
        assertThat(suggestions.value("$.dailyReviewMinutes")).isNull();
        assertThat(suggestions.body()).contains("复习额度");
    }

    @Test
    @DisplayName("概览与今日视图在未登录时不可读取")
    void businessEndpointsRequireLogin() {
        HttpTestClient anonymous = new HttpTestClient("http://127.0.0.1:" + port);

        assertThat(anonymous.get("/api/today").status()).isEqualTo(401);
        assertThat(anonymous.get("/api/progress/overview").status()).isEqualTo(401);
        assertThat(anonymous.get("/api/learning/records").status()).isEqualTo(401);
    }

    @Test
    void reviewCreationOverHttpRequiresLearnedSource() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long subjectId = createId("/api/settings/subjects", """
                {"name":"复习接口-%s","enabled":true}""".formatted(suffix));
        long unitId = createId("/api/settings/units", """
                {"name":"复习单位-%s","enabled":true}""".formatted(suffix));
        var missing = client.post("/api/review/items", """
                {"subjectId":%d,"title":"没有学习记录","estimatedMinutes":10}""".formatted(subjectId), csrf());
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.string("$.code")).isEqualTo("REVIEW_REQUIRES_STUDY");

        long recordId = createId("/api/learning/records", """
                {"subjectId":%d,"unitId":%d,"recordDate":"%s","amount":1,"durationMinutes":20,"content":"已学知识"}
                """.formatted(subjectId, unitId, LocalDate.now()));
        var review = client.post("/api/review/items", """
                {"sourceRecordId":%d,"title":"已学知识","estimatedMinutes":10}""".formatted(recordId), csrf());
        assertThat(review.status()).as(review.body()).isEqualTo(201);
        assertThat(review.number("$.subjectId").longValue()).isEqualTo(subjectId);

        // 旧任务路径仍接受没有 sourceRecordId 的 JSON。
        long taskId = createId("/api/plan/tasks", """
                {"subjectId":%d,"unitId":%d,"title":"已学任务","plannedAmount":2}""".formatted(subjectId, unitId));
        createId("/api/learning/records", """
                {"taskId":%d,"recordDate":"%s","amount":1,"durationMinutes":20}""".formatted(taskId, LocalDate.now()));
        var taskReview = client.post("/api/review/items", """
                {"taskId":%d,"estimatedMinutes":10}""".formatted(taskId), csrf());
        assertThat(taskReview.status()).as(taskReview.body()).isEqualTo(201);
    }

    private long createId(String path, String body) {
        HttpTestClient.Response response = client.post(path, body, csrf());
        assertThat(response.status()).as("创建 %s 应成功：%s", path, response.body()).isEqualTo(201);
        return response.number("$.id").longValue();
    }

    /** 每次写操作前取一次 CSRF 令牌：Cookie 由客户端自动携带。 */
    private String csrf() {
        return client.fetchCsrfToken();
    }

    /** 清空复习额度：验证“缺少额度时不宣称符合额度”的行为。 */
    private void clearReviewQuota() {
        HttpTestClient.Response current = client.get("/api/settings/study");
        long version = current.number("$.version").longValue();
        HttpTestClient.Response updated = client.put("/api/settings/study", """
                {"examDate":null,"dailyStudyMinutes":null,"dailyReviewMinutes":null,"reviewPhase":"MAIN",
                 "reviewIntervalForgotDays":null,"reviewIntervalVagueDays":null,"reviewIntervalMasteredDays":null,
                 "version":%d}""".formatted(version), csrf());
        assertThat(updated.status()).as("清空复习额度应成功：%s", updated.body()).isEqualTo(200);
    }
}
