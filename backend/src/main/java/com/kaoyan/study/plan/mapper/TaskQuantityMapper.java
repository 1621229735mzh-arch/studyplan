package com.kaoyan.study.plan.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface TaskQuantityMapper {
    @Select("SELECT COUNT(*) FROM daily_plan_item WHERE task_id=#{id}")
    int schedules(Long id);
    @Select("SELECT COUNT(*) FROM daily_plan_item WHERE task_id=#{id} AND source='REVIEW'")
    int reviewSchedules(Long id);
    @Select("SELECT COUNT(*) FROM weekly_plan_item WHERE task_id=#{id}")
    int weeklySchedules(Long id);
    @Select("SELECT COUNT(*) FROM study_record WHERE task_id=#{id}")
    int records(Long id);
    @Select("SELECT COUNT(*) FROM study_record WHERE task_id=#{id} AND amount IS NOT NULL")
    int learnedRecords(Long id);
    @Update("UPDATE daily_plan_item SET planned_amount=NULL,estimated_minutes=NULL,version=version+1 WHERE task_id=#{id}")
    int clearTargets(Long id);
}
