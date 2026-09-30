package com.kaoyan.study.today.service;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.entity.Subject;
import com.kaoyan.study.settings.service.StudySettingsService;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.today.dto.TodayChecklistTask;
import com.kaoyan.study.today.dto.TodayResponse;
import com.kaoyan.study.today.dto.TodaySubjectSection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 今日页面组合。
 *
 * <p>今日不另存一套任务：任务来自计划模块，完成量来自学习记录，复习任务来自已确认的复习项。
 */
@Service
public class TodayService {

    private final SubjectService subjectService;
    private final DailyPlanService dailyPlanService;
    private final StudyRecordService studyRecordService;
    private final ReviewService reviewService;
    private final StudySettingsService studySettingsService;

    public TodayService(DailyPlanService dailyPlanService,
                        StudyRecordService studyRecordService,
                        ReviewService reviewService,
                        StudySettingsService studySettingsService,
                        SubjectService subjectService) {
        this.subjectService = subjectService;
        this.dailyPlanService = dailyPlanService;
        this.studyRecordService = studyRecordService;
        this.reviewService = reviewService;
        this.studySettingsService = studySettingsService;
    }

    @Transactional
    public TodayResponse today(LocalDate date) {
        LocalDate planDate = date == null ? LocalDate.now() : date;

        List<DailyPlanItemView> plannedTasks = dailyPlanService.list(planDate);
        List<StudyRecordView> records = studyRecordService.list(planDate, planDate, null, null);
        List<ReviewItemResponse> reviewTasks = reviewService.scheduledOn(planDate).stream()
                .map(ReviewItemResponse::from)
                .toList();
        StudySettings settings = studySettingsService.get();

        int studyMinutes = records.stream()
                .map(StudyRecordView::durationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<String> warnings = new ArrayList<>();
        if (settings.getDailyStudyMinutes() == null) {
            warnings.add("尚未设置每日学习预算，无法判断今天是否排满");
        }
        if (settings.getDailyReviewMinutes() == null) {
            warnings.add("尚未设置每日复习额度，复习建议无法按额度校验");
        }

        var subjectNames = subjectService.list(false).stream().collect(Collectors.toMap(
                Subject::getId, Subject::getName));
        var groups = new LinkedHashMap<String, java.util.List<TodayChecklistTask>>();
        for (String name : java.util.List.of("数学一", "英语一", "政治", "408")) groups.put(name, new ArrayList<>());
        var actual = new HashMap<String, Integer>();
        var missingDurations = new HashMap<String, Integer>();
        for (var task : studyRecordService.dayTasks(planDate)) {
            String group = sectionName(subjectNames.get(task.subjectId()));
            groups.computeIfAbsent(group, x -> new ArrayList<>()).add(new TodayChecklistTask(
                    "LEARNING", task.id(), task.title(), task.completionPercent(), task.estimatedMinutes(),
                    task.remainingMinutes(), task.revision(), null, task.plannedAmount().signum() > 0));
        }
        for (var record : records) {
            String group = sectionName(subjectNames.get(record.subjectId()));
            groups.computeIfAbsent(group, x -> new ArrayList<>());
            actual.merge(group, record.durationMinutes() == null ? 0 : record.durationMinutes(), Integer::sum);
            if (record.durationMinutes() == null) missingDurations.merge(group, 1, Integer::sum);
        }
        for (var task : reviewService.dayTasks(planDate)) {
            String group = sectionName(subjectNames.get(task.subjectId()));
            var remaining = task.completionPercent().compareTo(new BigDecimal("100")) >= 0
                    ? BigDecimal.ZERO : task.estimatedMinutes() == null ? null
                    : BigDecimal.valueOf(task.estimatedMinutes()).multiply(BigDecimal.ONE
                        .subtract(task.completionPercent().movePointLeft(2))).setScale(1, RoundingMode.HALF_UP);
            if (task.actionable() || task.completionPercent().compareTo(new BigDecimal("100")) >= 0)
                groups.computeIfAbsent(group, x -> new ArrayList<>()).add(new TodayChecklistTask(
                    "REVIEW", task.id(), task.title(), task.completionPercent(), task.estimatedMinutes(),
                    remaining, null, task.version(), task.actionable()));
            groups.computeIfAbsent(group, x -> new ArrayList<>());
            actual.merge(group, task.actualMinutes(), Integer::sum);
            missingDurations.merge(group, task.missingDurations(), Integer::sum);
        }
        var sections = groups.entrySet().stream().map(entry -> {
            var remaining = entry.getValue().stream().map(TodayChecklistTask::remainingMinutes)
                    .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            int missing = (int) entry.getValue().stream().filter(x -> x.remainingMinutes() == null).count();
            return new TodaySubjectSection(entry.getKey(), actual.getOrDefault(entry.getKey(), 0),
                    remaining, missing, missingDurations.getOrDefault(entry.getKey(), 0), entry.getValue());
        }).toList();
        return new TodayResponse(planDate, plannedTasks, records, reviewTasks,
                settings.getDailyStudyMinutes(), settings.getDailyReviewMinutes(), studyMinutes, warnings,
                sections.stream().mapToInt(TodaySubjectSection::actualMinutes).sum(),
                sections.stream().map(TodaySubjectSection::remainingMinutes)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                sections.stream().mapToInt(TodaySubjectSection::missingEstimates).sum(),
                sections.stream().mapToInt(TodaySubjectSection::missingDurations).sum(), sections);
    }
    private String sectionName(String subject) {
        if (subject == null) return "未分类";
        if (subject.equals("数学") || subject.equals("数学一")) return "数学一";
        if (subject.equals("英语") || subject.equals("英语一")) return "英语一";
        if (subject.equals("政治")) return "政治";
        if (java.util.List.of("408", "数据结构", "计算机组成原理", "操作系统", "计算机网络").contains(subject)) return "408";
        return subject;
    }
}
