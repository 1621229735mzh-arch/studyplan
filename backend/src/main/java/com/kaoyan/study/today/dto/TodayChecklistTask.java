package com.kaoyan.study.today.dto;
import java.math.BigDecimal;
public record TodayChecklistTask(String kind, Long id, String title, BigDecimal completionPercent,
    Integer estimatedMinutes, BigDecimal remainingMinutes, String revision, Long version, boolean actionable,
    com.kaoyan.study.learning.service.SequenceLearningService.State sequence,
    com.kaoyan.study.plan.service.TaskActivityService.Activity activity,String unitName) {
    public TodayChecklistTask(String kind, Long id, String title, BigDecimal completionPercent,
        Integer estimatedMinutes, BigDecimal remainingMinutes, String revision, Long version, boolean actionable,
        com.kaoyan.study.learning.service.SequenceLearningService.State sequence) {
        this(kind,id,title,completionPercent,estimatedMinutes,remainingMinutes,revision,version,actionable,sequence,null,null);
    }
    public TodayChecklistTask(String kind, Long id, String title, BigDecimal completionPercent,
        Integer estimatedMinutes, BigDecimal remainingMinutes, String revision, Long version, boolean actionable) {
        this(kind,id,title,completionPercent,estimatedMinutes,remainingMinutes,revision,version,actionable,null,null,null);
    }
}
