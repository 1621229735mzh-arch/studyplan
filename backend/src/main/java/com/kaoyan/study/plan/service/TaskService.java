package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.plan.dto.TaskCreateRequest;
import com.kaoyan.study.plan.dto.TaskProgressView;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.entity.StageGoal;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.entity.TaskStatus;
import com.kaoyan.study.plan.mapper.StageGoalMapper;
import com.kaoyan.study.plan.mapper.TaskMapper;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 任务用例。
 *
 * <p>任务是周计划与今日安排共享的事实；创建或修改任务都不产生完成量，
 * 完成量只由学习记录决定。
 */
@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final StageGoalMapper stageGoalMapper;
    private final SubjectService subjectService;
    private final StudyUnitService studyUnitService;

    public TaskService(TaskMapper taskMapper,
                       StageGoalMapper stageGoalMapper,
                       SubjectService subjectService,
                       StudyUnitService studyUnitService) {
        this.taskMapper = taskMapper;
        this.stageGoalMapper = stageGoalMapper;
        this.subjectService = subjectService;
        this.studyUnitService = studyUnitService;
    }

    public List<Task> list(Long subjectId, Long stageGoalId, String status) {
        return taskMapper.findAll(subjectId, stageGoalId, status);
    }

    public Task get(Long id) {
        Task task = taskMapper.findById(id);
        if (task == null) {
            throw new NotFoundException("任务不存在");
        }
        return task;
    }

    /** 供学习记录与复习模块校验任务是否存在及其单位。 */
    public Task requireExisting(Long id) {
        return get(id);
    }

    /** 写学习记录和修改任务共用任务行锁，防止校验之后单位被另一事务改掉。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public Task requireExistingForUpdate(Long id) {
        Task task = taskMapper.findByIdForUpdate(id);
        if (task == null) {
            throw new NotFoundException("任务不存在");
        }
        return task;
    }

    public TaskProgressView progress(Long taskId) {
        get(taskId);
        return taskMapper.findProgress(taskId);
    }

    @Transactional
    public Task create(TaskCreateRequest request) {
        validateReferences(request.subjectId(), request.unitId(), request.stageGoalId());
        Task task = new Task();
        task.setSubjectId(request.subjectId());
        task.setStageGoalId(request.stageGoalId());
        task.setTitle(request.title().trim());
        task.setUnitId(request.unitId());
        task.setPlannedAmount(request.plannedAmount());
        task.setStatus(TaskStatus.ACTIVE);
        taskMapper.insert(task);
        return get(task.getId());
    }

    /** 供“备忘录转任务”等场景在既有事务中创建任务。 */
    @Transactional
    public Task create(Long subjectId, Long unitId, String title, BigDecimal plannedAmount) {
        return create(new TaskCreateRequest(subjectId, null, title, unitId, plannedAmount));
    }

    @Transactional
    public Task update(Long id, TaskUpdateRequest request) {
        Task task = requireExistingForUpdate(id);
        if (!task.getVersion().equals(request.version())) {
            throw new ConflictException("TASK_STALE", "该任务已在其他设备上修改，请刷新后重试");
        }
        validateReferences(request.subjectId(), request.unitId(), request.stageGoalId());
        if ((!task.getSubjectId().equals(request.subjectId()) || !task.getUnitId().equals(request.unitId()))
                && taskMapper.countRecords(id) > 0) {
            throw new ConflictException("TASK_MEASUREMENT_IN_USE", "该任务已有学习记录，不能修改科目或计量单位；请新建任务");
        }
        task.setSubjectId(request.subjectId());
        task.setStageGoalId(request.stageGoalId());
        task.setTitle(request.title().trim());
        task.setUnitId(request.unitId());
        task.setPlannedAmount(request.plannedAmount());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        int updated = taskMapper.update(task);
        if (updated == 0) {
            throw new ConflictException("TASK_STALE", "该任务已在其他设备上修改，请刷新后重试");
        }
        return get(id);
    }

    /**
     * 删除任务。
     *
     * <p>已有学习记录的任务不能删除：学习记录是既成事实，删掉任务会让记录失去归属，
     * 也会让总数对不上。这种情况应改为归档。任务的周计划/每日安排由外键级联清理。
     */
    @Transactional
    public void delete(Long id) {
        requireExistingForUpdate(id);
        if (taskMapper.countRecords(id) > 0) {
            throw new ConflictException("TASK_HAS_RECORDS", "该任务已有学习记录，不能删除；可改为归档");
        }
        try {
            taskMapper.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("TASK_IN_USE", "该任务仍被其他数据引用，不能删除；可改为归档");
        }
    }

    private void validateReferences(Long subjectId, Long unitId, Long stageGoalId) {
        subjectService.requireExisting(subjectId);
        studyUnitService.get(unitId);
        if (stageGoalId != null) {
            StageGoal goal = stageGoalMapper.findById(stageGoalId);
            if (goal == null) {
                throw new NotFoundException("阶段目标不存在");
            }
        }
    }
}
