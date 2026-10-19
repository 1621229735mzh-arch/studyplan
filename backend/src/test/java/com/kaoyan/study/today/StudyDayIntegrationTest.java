package com.kaoyan.study.today;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.progress.service.ProgressService;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.service.StudyDay;
import com.kaoyan.study.support.MySqlTestSupport;
import com.kaoyan.study.today.service.TodayService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StudyDayIntegrationTest {
    @Autowired TodayService today;
    @Autowired StudyRecordService records;
    @Autowired ProgressService progress;
    @Autowired ReviewService reviews;
    @MockitoBean StudyDay studyDay;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestSupport.registerDataSource(registry);
    }

    @Test
    void defaultsAndSavedRecordsFollowStudyDayWhileExplicitDatesStayIntact() {
        var previous = LocalDate.of(2026, 10, 18);
        var next = previous.plusDays(1);
        when(studyDay.today()).thenReturn(previous);
        var saved = records.create(new StudyRecordCreateRequest(null, null, null,
                previous, null, null, "凌晨六点学习", null, null));
        assertThat(today.today(null).date()).isEqualTo(previous);
        assertThat(today.today(null).records()).extracting(r -> r.id()).contains(saved.id());
        assertThat(progress.overview(null, null).to()).isEqualTo(previous);
        assertThat(reviews.suggestions(null).date()).isEqualTo(previous);

        when(studyDay.today()).thenReturn(next);
        assertThat(today.today(null).date()).isEqualTo(next);
        assertThat(today.today(null).records()).extracting(r -> r.id()).doesNotContain(saved.id());
        assertThat(progress.overview(null, null).to()).isEqualTo(next);
        assertThat(reviews.suggestions(null).date()).isEqualTo(next);
        assertThat(today.today(previous).records()).extracting(r -> r.id()).contains(saved.id());
        assertThat(records.get(saved.id()).recordDate()).isEqualTo(previous);
        assertThat(today.today(previous.minusDays(3)).date()).isEqualTo(previous.minusDays(3));
    }
}
