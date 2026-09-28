package com.kaoyan.study.today.service;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.service.ReviewService;
import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.service.StudySettingsService;
import com.kaoyan.study.today.dto.TodayResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 今日页面组合。
 *
 * <p>今日不另存一套任务：任务来自计划模块，完成量来自学习记录，复习任务来自已确认的复习项。
 */
@Service
public class TodayService {

    private final DailyPlanService dailyPlanService;
    private final StudyRecordService studyRecordService;
    private final ReviewService reviewService;
    private final StudySettingsService studySettingsService;

    public TodayService(DailyPlanService dailyPlanService,
                        StudyRecordService studyRecordService,
                        ReviewService reviewService,
                        StudySettingsService studySettingsService) {
        this.dailyPlanService = dailyPlanService;
        this.studyRecordService = studyRecordService;
        this.reviewService = reviewService;
        this.studySettingsService = studySettingsService;
    }

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
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<String> warnings = new ArrayList<>();
        if (settings.getDailyStudyMinutes() == null) {
            warnings.add("尚未设置每日学习预算，无法判断今天是否排满");
        }
        if (settings.getDailyReviewMinutes() == null) {
            warnings.add("尚未设置每日复习额度，复习建议无法按额度校验");
        }

        return new TodayResponse(planDate, plannedTasks, records, reviewTasks,
                settings.getDailyStudyMinutes(), settings.getDailyReviewMinutes(), studyMinutes, warnings);
    }
}
