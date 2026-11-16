package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.entity.TaskStatus;
import com.kaoyan.study.plan.mapper.WorkspaceMapper;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;

/** Opt in an existing self-selected practice; never rebuild or replace learning history. */
@Service
public class DailyPracticeService {
    public record Request(@NotNull Long version, @NotNull LocalDate from,
                          @NotBlank @Size(max=200) String title) {}
    private final TaskService tasks;
    private final TaskSequenceService sequences;
    private final WorkspaceMapper workspace;
    public DailyPracticeService(TaskService tasks,TaskSequenceService sequences,WorkspaceMapper workspace) {
        this.tasks=tasks;this.sequences=sequences;this.workspace=workspace;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void enable(Long taskId,Request request) {
        workspace.lock();
        var task=tasks.requireExistingForUpdate(taskId);
        if (!task.getVersion().equals(request.version()))
            throw new ConflictException("TASK_STALE","任务已在其他设备修改，请刷新后重试");
        var sequence=sequences.definition(taskId);
        if (sequence==null || !"SELECT_BY_NUMBER".equals(sequence.mode()) || sequence.dailyFrom()!=null
                || task.getStatus()!=TaskStatus.ACTIVE)
            throw new ConflictException("DAILY_PRACTICE_SOURCE","请选择尚未转换的自填题号任务");
        if (request.from().isBefore(sequence.startDate()))
            throw new ConflictException("DAILY_PRACTICE_START","生效日期不能早于原练习开始日期");
        sequences.startDailyPractice(taskId,request.from());
        tasks.update(taskId,new TaskUpdateRequest(task.getSubjectId(),null,request.title(),task.getUnitId(),
                null,task.getStatus(),request.version()));
        workspace.touch();
    }
}
