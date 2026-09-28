package com.kaoyan.study.memo;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.memo.dto.MemoConvertRequest;
import com.kaoyan.study.memo.dto.MemoRequest;
import com.kaoyan.study.memo.service.MemoService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.settings.dto.StudyUnitRequest;
import com.kaoyan.study.settings.dto.SubjectRequest;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.support.MySqlTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MemoConversionIntegrationTest {
    @Autowired private MemoService memoService;
    @Autowired private TaskService taskService;
    @Autowired private SubjectService subjectService;
    @Autowired private StudyUnitService unitService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
        registry.add("app.preset-account.username", () -> "kaoyan");
        registry.add("app.preset-account.password", () -> "test-only-password");
    }

    @Test
    void concurrentConversionAndRetryCreateOnlyOneTask() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var subject = subjectService.create(new SubjectRequest("备忘录-" + suffix, null, null, 0, true));
        var unit = unitService.create(new StudyUnitRequest("题-" + suffix, 0, true));
        var memo = memoService.create(new MemoRequest(subject.getId(), "只转一次", null, null, 0L));
        var request = new MemoConvertRequest("复习笔记", subject.getId(), unit.getId(), BigDecimal.ONE);
        CyclicBarrier ready = new CyclicBarrier(2);
        Long taskId;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                ready.await(10, TimeUnit.SECONDS);
                return memoService.convertToTask(memo.getId(), request);
            });
            var second = executor.submit(() -> {
                ready.await(10, TimeUnit.SECONDS);
                return memoService.convertToTask(memo.getId(), request);
            });
            taskId = first.get(20, TimeUnit.SECONDS).getConvertedTaskId();
            assertThat(second.get(20, TimeUnit.SECONDS).getConvertedTaskId()).isEqualTo(taskId);
        }
        assertThat(memoService.convertToTask(memo.getId(), request).getConvertedTaskId()).isEqualTo(taskId);
        assertThat(taskService.list(subject.getId(), null, null)).hasSize(1);
        assertThat(memoService.get(memo.getId()).getVersion()).isEqualTo(memo.getVersion() + 1);
        assertThatThrownBy(() -> taskService.delete(taskId)).isInstanceOf(ConflictException.class);
    }
}
