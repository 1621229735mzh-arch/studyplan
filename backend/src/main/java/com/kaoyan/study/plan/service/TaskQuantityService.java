package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.mapper.TaskQuantityMapper;
import com.kaoyan.study.plan.mapper.WorkspaceMapper;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Explicit conversion of an existing task; never infers quantities from old check marks. */
@Service
public class TaskQuantityService {
    public record Preview(Long taskId, String title, String mode, Long version, long workspaceRevision,
                          int scheduleCount, int preservedRecordCount) {}
    public record Confirm(@NotBlank @Size(max=200) String title, @NotNull Long version,
                          @NotNull Long workspaceRevision, @NotBlank String mode) {}
    private final TaskService tasks;
    private final TaskActivityService activities;
    private final TaskSequenceService sequences;
    private final TaskQuantityMapper mapper;
    private final WorkspaceMapper workspace;
    public TaskQuantityService(TaskService tasks,TaskActivityService activities,TaskSequenceService sequences,
                               TaskQuantityMapper mapper,WorkspaceMapper workspace) {
        this.tasks=tasks;this.activities=activities;this.sequences=sequences;this.mapper=mapper;this.workspace=workspace;
    }
    @Transactional(readOnly=true)
    public Preview preview(Long id) {
        var task=tasks.get(id);
        var old=activities.get(id);
        if (sequences.definition(id)!=null || old!=null && !java.util.Set.of("TIMED_STUDY","RECAP","CHECK_ONLY").contains(old.mode())
                || mapper.reviewSchedules(id)>0 || mapper.weeklySchedules(id)>0)
            throw new ConflictException("COUNT_MODE_UNSUPPORTED","此任务不适合转换为每日实际数量安排");
        String mode=old!=null && java.util.Set.of("RECAP","CHECK_ONLY").contains(old.mode()) ? "COUNT_REVIEW":"COUNT_STUDY";
        if ("COUNT_REVIEW".equals(mode) && mapper.learnedRecords(id)>0)
            throw new ConflictException("COUNT_MODE_HISTORY","该任务含新学数量，不能转换为复习数量");
        return new Preview(id,task.getTitle(),mode,task.getVersion(),workspace.revision(),mapper.schedules(id),mapper.records(id));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void confirm(Long id,Confirm request) {
        workspace.lock();
        var task=tasks.requireExistingForUpdate(id);
        var current=preview(id);
        if (!current.version().equals(request.version()) || current.workspaceRevision()!=request.workspaceRevision()
                || !current.mode().equals(request.mode()))
            throw new ConflictException("COUNT_MODE_STALE","任务或日安排已变化，请重新预览");
        tasks.update(id,new TaskUpdateRequest(task.getSubjectId(),task.getStageGoalId(),request.title(),task.getUnitId(),null,task.getStatus(),task.getVersion()));
        activities.replace(id,new TaskActivityService.Activity(current.mode(),
                "COUNT_REVIEW".equals(current.mode()) ? "填写实际复习数量，不增加新词量；未复习不积压":"填写实际新学数量，不设每日目标；未学习不积压",0));
        mapper.clearTargets(id);
        workspace.touch();
    }
}
