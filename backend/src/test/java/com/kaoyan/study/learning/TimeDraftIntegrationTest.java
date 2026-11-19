package com.kaoyan.study.learning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.DailyItemCreateRequest;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.support.HttpTestClient;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.preset-account.username=kaoyan", "app.preset-account.password=test-only-password"})
class TimeDraftIntegrationTest {
    @DynamicPropertySource
    static void db(DynamicPropertyRegistry registry) { MySqlTestSupport.registerDataSource(registry); }

    @LocalServerPort int port;
    @Autowired SubjectService subjects;
    @Autowired StudyUnitService units;
    @Autowired TaskService tasks;
    @Autowired DailyPlanService daily;
    @Autowired StudyRecordService records;

    private HttpTestClient login() {
        var client = new HttpTestClient("http://127.0.0.1:" + port);
        assertThat(client.post("/api/auth/login", """
                {"username":"kaoyan","password":"test-only-password"}
                """, client.fetchCsrfToken()).status()).isEqualTo(200);
        return client;
    }

    @Test
    void dayProgressPreservesDraftOnConflictAndRecordsMinutesWithoutAdvancing() {
        var client = login();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var subject = subjects.create(new SubjectRequest("高数-" + suffix, null, null, 0, true));
        var unit = units.create(new StudyUnitRequest("段-" + suffix, 0, true));
        var task = tasks.create(subject.getId(), unit.getId(), "高数练习", BigDecimal.ONE);
        var day = LocalDate.of(2026, 11, 18);
        daily.addItem(day, new DailyItemCreateRequest(task.getId(), BigDecimal.ONE, null));
        String revision = records.dayTasks(day).stream().filter(item -> item.id().equals(task.getId()))
                .findFirst().orElseThrow().revision();
        String path = "/api/learning/time-drafts/task-" + task.getId();
        var draft = client.put(path, "{\"startedAt\":\"2026-11-18T14:10\"}", client.fetchCsrfToken());
        assertThat(draft.status()).as(draft.body()).isEqualTo(200);
        String draftRevision = draft.string("$.revision");
        String body = """
                {"taskId":%d,"date":"%s","completionPercent":0,"durationMinutes":42,"revision":"%s","clientToken":"%s",
                 "timeRange":{"revision":"%s","endedAt":"2026-11-18T14:52"}}
                """.formatted(task.getId(), day, revision, UUID.randomUUID(), draftRevision);
        String progress = "/api/learning/records/day-progress";
        assertThat(client.post(progress, body.replace(revision, "stale"), client.fetchCsrfToken()).status()).isEqualTo(409);
        assertThat(client.get(path).string("$.draft.revision")).isEqualTo(draftRevision);
        assertThat(records.list(day, day, task.getId(), null)).isEmpty();
        var saved = client.post(progress, body, client.fetchCsrfToken());
        assertThat(saved.status()).as(saved.body()).isEqualTo(201);
        assertThat(saved.number("$.durationMinutes").intValue()).isEqualTo(42);
        assertThat(client.post(progress, body, client.fetchCsrfToken()).number("$.id")).isEqualTo(saved.number("$.id"));
        assertThat(tasks.progress(task.getId()).completedAmount()).isEqualByComparingTo("0");
        assertThat(records.list(day, day, task.getId(), null)).hasSize(1);
        assertThat(client.get(path).value("$.draft")).isNull();
    }

    @Test
    void resumeAfterLogoutAndConsumeOnlyOnceWithTheRecord() {
        var first = login();
        var subject = subjects.create(new SubjectRequest("数学-" + UUID.randomUUID().toString().substring(0,8), null, null, 0, true));
        String path = "/api/learning/time-drafts/subject-" + subject.getId();
        String start = """
                {"startedAt":"2026-11-18T23:45"}
                """;
        var saved = first.put(path, start, first.fetchCsrfToken());
        assertThat(saved.status()).as(saved.body()).isEqualTo(200);
        String revision = saved.string("$.revision");
        assertThat(first.post("/api/auth/logout", "{}", first.fetchCsrfToken()).status()).isIn(200, 204);
        assertThat(first.get(path).status()).isEqualTo(401);
        var next = login();
        assertThat(next.get(path).string("$.draft.revision")).isEqualTo(revision);
        assertThat(next.put(path, start, next.fetchCsrfToken()).status()).isEqualTo(409);
        String body = """
                {"subjectId":%d,"recordDate":"2026-11-18","content":"跨午夜学习","clientToken":"%s","durationMinutes":35,
                 "timeRange":{"revision":"%s","endedAt":"2026-11-19T00:20"}}
                """.formatted(subject.getId(), UUID.randomUUID(), revision);
        String records = "/api/learning/records";
        assertThat(next.post(records, body.replace("00:20", "00:21"), next.fetchCsrfToken()).status()).isEqualTo(400);
        assertThat(next.get(path).string("$.draft.revision")).isEqualTo(revision);
        var record = next.post(records, body, next.fetchCsrfToken());
        assertThat(record.status()).as(record.body()).isEqualTo(201);
        assertThat(record.number("$.durationMinutes").intValue()).isEqualTo(35);
        assertThat(next.get(path).value("$.draft")).isNull();
        assertThat(next.post(records, body, next.fetchCsrfToken()).number("$.id")).isEqualTo(record.number("$.id"));
        assertThat(next.post(records, body.replace("跨午夜学习","另一笔").replaceFirst("\"clientToken\":\"[^\"]+\"",
                "\"clientToken\":\"" + UUID.randomUUID() + "\""), next.fetchCsrfToken()).status()).isEqualTo(409);
    }
}
