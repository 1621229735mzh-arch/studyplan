package com.kaoyan.study.progress.mapper;

import com.kaoyan.study.progress.dto.DailyUnitAmountView;
import com.kaoyan.study.progress.dto.UnitProgressView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 进度统计的只读关联查询。
 *
 * <p>计划量来自 task，完成量来自 study_record；按“科目 + 单位”分组，
 * 从设计上保证不同单位不会被相加。
 */
@Mapper
public interface ProgressMapper {

    List<UnitProgressView> findUnitProgress(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 某天的学习时长合计（分钟）。 */
    List<DailyMinutesRow> findDailyMinutes(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<DailyUnitAmountRow> findDailyUnitAmounts(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** 按天统计时长。 */
    record DailyMinutesRow(LocalDate date, Integer studyMinutes) {
    }

    /** 按天 + 单位统计完成量。 */
    record DailyUnitAmountRow(LocalDate date, Long unitId, String unitName, java.math.BigDecimal amount) {
    }
}
