package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.plan.dto.DailyItemAdjustRequest;
import com.kaoyan.study.plan.dto.DailyItemCreateRequest;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.entity.DailyPlanItem;
import com.kaoyan.study.plan.entity.PlanSource;
import com.kaoyan.study.plan.mapper.PlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 每日安排用例。
 *
 * <p>安排只是“打算做多少”，完成量永远来自学习记录。未完成的安排不会自动顺延到次日：
 * 需要继续做时由本人重新安排或调整当天计划量。
 */
@Service
public class DailyPlanService {

    private final PlanMapper planMapper;
    private final TaskService taskService;

    public DailyPlanService(PlanMapper planMapper, TaskService taskService) {
        this.planMapper = planMapper;
        this.taskService = taskService;
    }

    public List<DailyPlanItemView> list(LocalDate date) {
        return planMapper.findDailyItems(date);
    }

    /** 加入当天安排；同一任务同一来源已存在时更新计划量。 */
    @Transactional
    public List<DailyPlanItemView> addItem(LocalDate date, DailyItemCreateRequest request) {
        taskService.requireExisting(request.taskId());
        PlanSource source = request.sourceOrDefault();
        DailyPlanItem existing = planMapper.findDailyItem(date, request.taskId(), source.name());
        if (existing == null) {
            DailyPlanItem item = new DailyPlanItem();
            item.setPlanDate(date);
            item.setTaskId(request.taskId());
            item.setPlannedAmount(request.plannedAmount());
            item.setSource(source);
            planMapper.insertDailyItem(item);
        } else {
            existing.setPlannedAmount(request.plannedAmount());
            if (planMapper.updateDailyItemAmount(existing) == 0) {
                throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
            }
        }
        return list(date);
    }

    /** 手动调整当天安排量，并留下调整记录（含调整前/后数量与原因）。 */
    @Transactional
    public List<DailyPlanItemView> adjust(LocalDate date, Long itemId, DailyItemAdjustRequest request) {
        DailyPlanItem item = planMapper.findDailyItemById(itemId);
        if (item == null || !item.getPlanDate().equals(date)) {
            throw new NotFoundException("当天安排不存在");
        }
        if (!item.getVersion().equals(request.version())) {
            throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
        }
        BigDecimal before = item.getPlannedAmount();
        item.setPlannedAmount(request.plannedAmount());
        if (planMapper.updateDailyItemAmount(item) == 0) {
            throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
        }
        planMapper.insertAdjustment(null, date, item.getTaskId(), "DAILY_AMOUNT",
                before, request.plannedAmount(), request.reason());
        return list(date);
    }

    @Transactional
    public List<DailyPlanItemView> removeItem(LocalDate date, Long itemId) {
        DailyPlanItem item = planMapper.findDailyItemById(itemId);
        if (item == null || !item.getPlanDate().equals(date)) {
            throw new NotFoundException("当天安排不存在");
        }
        planMapper.deleteDailyItem(itemId);
        return list(date);
    }
}
