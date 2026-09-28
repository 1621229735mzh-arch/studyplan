package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.plan.dto.WeeklyPlanItemRequest;
import com.kaoyan.study.plan.dto.WeeklyPlanItemView;
import com.kaoyan.study.plan.dto.WeeklyPlanResponse;
import com.kaoyan.study.plan.entity.WeeklyPlan;
import com.kaoyan.study.plan.mapper.PlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * 周计划用例。
 *
 * <p>周计划引用任务而不是复制任务，因此这里的计划量与今日安排、任务进度始终指向同一事实。
 */
@Service
public class WeeklyPlanService {

    private final PlanMapper planMapper;
    private final TaskService taskService;

    public WeeklyPlanService(PlanMapper planMapper, TaskService taskService) {
        this.planMapper = planMapper;
        this.taskService = taskService;
    }

    /** 读取某一周的计划，不存在时创建空计划。 */
    @Transactional
    public WeeklyPlanResponse getOrCreate(LocalDate weekStart) {
        requireMonday(weekStart);
        WeeklyPlan plan = planMapper.findWeeklyPlan(weekStart);
        if (plan == null) {
            WeeklyPlan created = new WeeklyPlan();
            created.setWeekStartDate(weekStart);
            planMapper.insertWeeklyPlan(created);
            plan = planMapper.findWeeklyPlanById(created.getId());
        }
        List<WeeklyPlanItemView> items = planMapper.findWeeklyItems(plan.getId());
        return new WeeklyPlanResponse(plan.getWeekStartDate(), plan.getNote(), plan.getVersion(), items);
    }

    /** 安排任务到本周；同一任务重复安排时更新计划量。 */
    @Transactional
    public WeeklyPlanResponse addOrUpdateItem(LocalDate weekStart, WeeklyPlanItemRequest request) {
        requireMonday(weekStart);
        taskService.requireExisting(request.taskId());
        WeeklyPlan plan = requirePlan(weekStart);
        int updated = planMapper.updateWeeklyItemAmount(plan.getId(), request.taskId(), request.plannedAmount());
        if (updated == 0) {
            planMapper.insertWeeklyItem(plan.getId(), request.taskId(), request.plannedAmount());
        }
        return getOrCreate(weekStart);
    }

    @Transactional
    public WeeklyPlanResponse removeItem(LocalDate weekStart, Long taskId) {
        requireMonday(weekStart);
        WeeklyPlan plan = requirePlan(weekStart);
        planMapper.deleteWeeklyItem(plan.getId(), taskId);
        return getOrCreate(weekStart);
    }

    private WeeklyPlan requirePlan(LocalDate weekStart) {
        WeeklyPlan plan = planMapper.findWeeklyPlan(weekStart);
        if (plan == null) {
            WeeklyPlan created = new WeeklyPlan();
            created.setWeekStartDate(weekStart);
            planMapper.insertWeeklyPlan(created);
            plan = planMapper.findWeeklyPlanById(created.getId());
        }
        if (plan == null) {
            throw new IllegalStateException("周计划创建失败");
        }
        return plan;
    }

    private void requireMonday(LocalDate weekStart) {
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BusinessException("WEEK_START_NOT_MONDAY", "周计划以周一日期标识，请传入周一");
        }
    }
}
