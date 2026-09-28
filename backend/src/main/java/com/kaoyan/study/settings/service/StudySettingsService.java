package com.kaoyan.study.settings.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.settings.dto.StudySettingsRequest;
import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.mapper.StudySettingsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * 学习设置用例。
 *
 * <p>预算与复习额度保持“可为空”的语义：空值表示本人尚未确定，
 * 后续复习额度校验必须据此提示补充，而不是按 0 处理。
 */
@Service
public class StudySettingsService {

    private final StudySettingsMapper studySettingsMapper;

    public StudySettingsService(StudySettingsMapper studySettingsMapper) {
        this.studySettingsMapper = studySettingsMapper;
    }

    /** 读取设置；首次访问时创建默认行。 */
    @Transactional
    public StudySettings get() {
        StudySettings settings = studySettingsMapper.find();
        if (settings == null) {
            studySettingsMapper.insertDefaultIfAbsent();
            settings = studySettingsMapper.find();
        }
        if (settings == null) {
            throw new IllegalStateException("学习设置行初始化失败");
        }
        return settings;
    }

    /** 额度校验持有设置行锁至外层事务提交，串行化确认安排和额度修改。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public StudySettings lockForReviewScheduling() {
        StudySettings settings = studySettingsMapper.findForUpdate();
        if (settings == null) {
            studySettingsMapper.insertDefaultIfAbsent();
            settings = studySettingsMapper.findForUpdate();
        }
        if (settings == null) {
            throw new IllegalStateException("学习设置行初始化失败");
        }
        return settings;
    }

    /** 更新设置；版本不一致说明另一台设备已修改，拒绝静默覆盖。 */
    @Transactional
    public StudySettings update(StudySettingsRequest request) {
        StudySettings current = get();
        if (!current.getVersion().equals(request.version())) {
            throw new ConflictException("SETTINGS_STALE",
                    "设置已在其他设备上修改，请刷新后重试");
        }
        current.setExamDate(request.examDate());
        current.setDailyStudyMinutes(request.dailyStudyMinutes());
        current.setDailyReviewMinutes(request.dailyReviewMinutes());
        if (request.reviewPhase() != null) {
            current.setReviewPhase(request.reviewPhase());
        }
        // 复习间隔是待定项，允许保持为空：为空时复习模块不给出具体日期建议。
        current.setReviewIntervalForgotDays(request.reviewIntervalForgotDays());
        current.setReviewIntervalVagueDays(request.reviewIntervalVagueDays());
        current.setReviewIntervalMasteredDays(request.reviewIntervalMasteredDays());
        int updated = studySettingsMapper.update(current);
        if (updated == 0) {
            throw new ConflictException("SETTINGS_STALE", "设置已在其他设备上修改，请刷新后重试");
        }
        return get();
    }
}
