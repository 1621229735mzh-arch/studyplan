package com.kaoyan.study.plan.service;

import com.kaoyan.study.plan.mapper.TaskActivityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Planning metadata only; completed activities are derived from learning records. */
@Service
public class TaskActivityService {
    public record Activity(String mode, String description, Integer flexibleMinutes) {}
    public static boolean counted(Activity activity) {
        return activity != null && ("COUNT_STUDY".equals(activity.mode()) || "COUNT_REVIEW".equals(activity.mode()));
    }
    private final TaskActivityMapper mapper;
    public TaskActivityService(TaskActivityMapper mapper) { this.mapper=mapper; }
    public Activity get(Long taskId) { return mapper.find(taskId); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void create(Long taskId,Activity activity) { mapper.insert(taskId,activity); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void replace(Long taskId,Activity activity) {
        if (mapper.find(taskId)==null) mapper.insert(taskId,activity); else mapper.update(taskId,activity);
    }
}
