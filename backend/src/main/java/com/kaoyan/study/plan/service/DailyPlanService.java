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
import org.springframework.transaction.annotation.Isolation;
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
    private final com.kaoyan.study.plan.mapper.WorkspaceMapper workspace;

    public DailyPlanService(PlanMapper planMapper, TaskService taskService, com.kaoyan.study.plan.mapper.WorkspaceMapper workspace) {
        this.planMapper = planMapper;
        this.taskService = taskService;
        this.workspace = workspace;
    }

    public List<DailyPlanItemView> list(LocalDate date) {
        return planMapper.findDailyItems(date);
    }

    /** 新增安排；已有条目须经携带版本的调整接口修改。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DailyPlanItemView> addItem(LocalDate date, DailyItemCreateRequest request) {
        if (request.sourceOrDefault() == PlanSource.REVIEW || request.sourceOrDefault() == PlanSource.IMPORT)
            throw new ConflictException("SOURCE_MANAGED_SEPARATELY", "请通过复习确认或外部方案导入入口添加此来源的安排");
        writeItem(date,request);
        return list(date);
    }

    /** 整份方案导入中无需每次回读当天全表，防止大文件出现平方级查询开销。 */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void appendImported(LocalDate date, DailyItemCreateRequest request) {
        if (request.sourceOrDefault()!=PlanSource.IMPORT) throw new IllegalArgumentException("Import source required");
        writeItem(date,request);
    }

    private void writeItem(LocalDate date, DailyItemCreateRequest request) {
        workspace.lock();
        taskService.requireExistingForUpdate(request.taskId());
        PlanSource source = request.sourceOrDefault();
        DailyPlanItem existing = planMapper.findDailyItem(date, request.taskId(), source.name());
        if (existing == null) {
            int order = workspace.nextOrder(date);
            DailyPlanItem item = new DailyPlanItem();
            item.setPlanDate(date);
            item.setTaskId(request.taskId());
            item.setPlannedAmount(request.plannedAmount());
            item.setEstimatedMinutes(request.estimatedMinutes());
            item.setSource(source);
            planMapper.insertDailyItem(item);
            workspace.initializeOrder(item.getId(), order);
        } else {
            throw new ConflictException("TASK_ALREADY_SCHEDULED", "该任务当天已有同来源安排，请刷新后使用编辑入口");
        }
        workspace.touch();
    }

    /** 手动调整当天安排量，并留下调整记录（含调整前/后数量与原因）。 */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DailyPlanItemView> adjust(LocalDate date, Long itemId, DailyItemAdjustRequest request) {
        workspace.lock();
        DailyPlanItem item = planMapper.findDailyItemById(itemId);
        if (item == null || !item.getPlanDate().equals(date)) {
            throw new NotFoundException("当天安排不存在");
        }
        taskService.requireExistingForUpdate(item.getTaskId());
        item = planMapper.findDailyItemById(itemId);
        if (item == null || !item.getVersion().equals(request.version())) {
            throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
        }
        BigDecimal before = item.getPlannedAmount();
        if (item.getSource() == PlanSource.REVIEW) {
            throw new ConflictException("REVIEW_MANAGED_SEPARATELY", "请在复习页面调整复习安排");
        }
        item.setPlannedAmount(request.plannedAmount());
        item.setEstimatedMinutes(request.estimatedMinutes());
        if (planMapper.updateDailyItemAmount(item) == 0) {
            throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
        }
        planMapper.insertAdjustment(null, date, item.getTaskId(), "DAILY_AMOUNT",
                before, request.plannedAmount(), request.reason());
        workspace.touch();
        return list(date);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DailyPlanItemView> removeItem(LocalDate date, Long itemId, Long version) {
        workspace.lock();
        DailyPlanItem item = planMapper.findDailyItemById(itemId);
        if (item == null || !item.getPlanDate().equals(date)) {
            throw new NotFoundException("当天安排不存在");
        }
        taskService.requireExistingForUpdate(item.getTaskId());
        if (!item.getVersion().equals(version)) {
            throw new ConflictException("DAILY_ITEM_STALE", "当天安排已在其他设备上修改，请刷新后重试");
        }
        if (item.getSource() == PlanSource.REVIEW) {
            throw new ConflictException("REVIEW_MANAGED_SEPARATELY", "请在复习页面管理复习安排");
        }
        planMapper.deleteDailyItem(itemId);
        workspace.touch();
        return list(date);
    }
}
