package com.kaoyan.study.learning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 所有汇总由后端生成；单位分开，未填用时不伪装为 0。 */
public record RecordArchive(Stats stats, List<Month> months) {
    public record Amount(Long subjectId, String subjectName, Long unitId, String unitName, BigDecimal amount) {}
    public record Stats(int learningDays, int recordCount, Long studyMinutes, int missingDurationRecords, List<Amount> amounts) {}
    public record Month(String month, Stats stats, List<Week> weeks) {}
    public record Week(LocalDate weekStart, LocalDate from, LocalDate to, Stats stats, List<Day> days) {}
    public record Day(LocalDate date, Stats stats, List<Group> groups) {}
    public record Group(String key, Long subjectId, String subjectName, Long courseId, String title,
                        Long unitId, String unitName, BigDecimal amount, Long studyMinutes,
                        int missingDurationRecords, List<StudyRecordView> records) {}
}
