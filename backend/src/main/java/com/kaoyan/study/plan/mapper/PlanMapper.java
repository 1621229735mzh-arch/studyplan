package com.kaoyan.study.plan.mapper;

import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.dto.WeeklyPlanItemView;
import com.kaoyan.study.plan.entity.DailyPlanItem;
import com.kaoyan.study.plan.entity.WeeklyPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 周计划、每日安排与调整记录的数据访问。 */
@Mapper
public interface PlanMapper {

    // ---- 周计划 ----

    WeeklyPlan findWeeklyPlan(@Param("weekStartDate") LocalDate weekStartDate);

    WeeklyPlan findWeeklyPlanById(@Param("id") Long id);

    int insertWeeklyPlan(WeeklyPlan plan);

    int updateWeeklyPlanNote(WeeklyPlan plan);

    List<WeeklyPlanItemView> findWeeklyItems(@Param("weeklyPlanId") Long weeklyPlanId);

    int insertWeeklyItem(@Param("weeklyPlanId") Long weeklyPlanId,
                         @Param("taskId") Long taskId,
                         @Param("plannedAmount") BigDecimal plannedAmount);

    int updateWeeklyItemAmount(@Param("weeklyPlanId") Long weeklyPlanId,
                               @Param("taskId") Long taskId,
                               @Param("plannedAmount") BigDecimal plannedAmount);

    int deleteWeeklyItem(@Param("weeklyPlanId") Long weeklyPlanId, @Param("taskId") Long taskId);

    // ---- 每日安排 ----

    List<DailyPlanItemView> findDailyItems(@Param("planDate") LocalDate planDate);

    DailyPlanItem findDailyItemById(@Param("id") Long id);

    DailyPlanItem findDailyItem(@Param("planDate") LocalDate planDate,
                                @Param("taskId") Long taskId,
                                @Param("source") String source);

    int insertDailyItem(DailyPlanItem item);

    /** 按 id + version 更新计划量。 */
    int updateDailyItemAmount(DailyPlanItem item);

    int deleteDailyItem(@Param("id") Long id);

    int deleteDailyItemByTaskAndSource(@Param("planDate") LocalDate planDate,
                                       @Param("taskId") Long taskId,
                                       @Param("source") String source);

    // ---- 调整记录 ----

    int insertAdjustment(@Param("applyToken") String applyToken,
                         @Param("planDate") LocalDate planDate,
                         @Param("taskId") Long taskId,
                         @Param("adjustmentType") String adjustmentType,
                         @Param("beforeAmount") BigDecimal beforeAmount,
                         @Param("afterAmount") BigDecimal afterAmount,
                         @Param("reason") String reason);

    long countAdjustmentByToken(@Param("applyToken") String applyToken);
}
