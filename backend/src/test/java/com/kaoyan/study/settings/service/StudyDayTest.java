package com.kaoyan.study.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StudyDayTest {
    @ParameterizedTest
    @CsvSource({
        "2026-10-18T16:00:00Z, 2026-10-18",
        "2026-10-18T22:00:00Z, 2026-10-18",
        "2026-10-19T00:59:59.999999999Z, 2026-10-18",
        "2026-10-19T01:00:00Z, 2026-10-19",
        "2026-10-19T01:00:00.000000001Z, 2026-10-19",
        "2026-10-31T22:00:00Z, 2026-10-31",
        "2026-12-31T22:00:00Z, 2026-12-31",
        "2028-02-29T22:00:00Z, 2028-02-29"
    })
    void resolvesShanghaiStudyDayRegardlessOfHostZone(String instant, String expected) {
        var clock = Clock.fixed(Instant.parse(instant), ZoneId.of("America/Los_Angeles"));
        assertThat(new StudyDay(clock).today()).hasToString(expected);
    }
}
