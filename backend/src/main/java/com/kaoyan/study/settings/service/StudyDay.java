package com.kaoyan.study.settings.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/** 学习日按北京时间 09:00 切换；显式选择的补记日期不做偏移。 */
@Component
public class StudyDay {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final Clock clock;

    public StudyDay() {
        this(Clock.systemUTC());
    }

    StudyDay(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return ZonedDateTime.now(clock.withZone(ZONE)).minusHours(9).toLocalDate();
    }
}
