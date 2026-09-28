package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.plan.dto.StageGoalCreateRequest;
import com.kaoyan.study.plan.dto.StageGoalUpdateRequest;
import com.kaoyan.study.plan.entity.StageGoal;
import com.kaoyan.study.plan.mapper.StageGoalMapper;
import com.kaoyan.study.settings.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 阶段目标用例。 */
@Service
public class StageGoalService {

    private final StageGoalMapper stageGoalMapper;
    private final SubjectService subjectService;

    public StageGoalService(StageGoalMapper stageGoalMapper, SubjectService subjectService) {
        this.stageGoalMapper = stageGoalMapper;
        this.subjectService = subjectService;
    }

    public List<StageGoal> list(Long subjectId, String status) {
        return stageGoalMapper.findAll(subjectId, status);
    }

    public StageGoal get(Long id) {
        StageGoal goal = stageGoalMapper.findById(id);
        if (goal == null) {
            throw new NotFoundException("阶段目标不存在");
        }
        return goal;
    }

    @Transactional
    public StageGoal create(StageGoalCreateRequest request) {
        subjectService.requireExisting(request.subjectId());
        StageGoal goal = new StageGoal();
        goal.setSubjectId(request.subjectId());
        goal.setTitle(request.title().trim());
        goal.setDescription(request.description());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        goal.setStatus("ACTIVE");
        stageGoalMapper.insert(goal);
        return get(goal.getId());
    }

    @Transactional
    public StageGoal update(Long id, StageGoalUpdateRequest request) {
        StageGoal goal = get(id);
        if (!goal.getVersion().equals(request.version())) {
            throw new ConflictException("STAGE_GOAL_STALE", "该阶段目标已在其他设备上修改，请刷新后重试");
        }
        subjectService.requireExisting(request.subjectId());
        goal.setSubjectId(request.subjectId());
        goal.setTitle(request.title().trim());
        goal.setDescription(request.description());
        goal.setStartDate(request.startDate());
        goal.setTargetDate(request.targetDate());
        if (request.status() != null) {
            goal.setStatus(request.status());
        }
        int updated = stageGoalMapper.update(goal);
        if (updated == 0) {
            throw new ConflictException("STAGE_GOAL_STALE", "该阶段目标已在其他设备上修改，请刷新后重试");
        }
        return get(id);
    }

    /** 删除阶段目标；已被任务引用时拒绝，避免任务失去目标归属。 */
    @Transactional
    public void delete(Long id) {
        get(id);
        if (stageGoalMapper.countTasks(id) > 0) {
            throw new ConflictException("STAGE_GOAL_IN_USE", "该目标下已有任务，不能删除");
        }
        stageGoalMapper.deleteById(id);
    }
}
