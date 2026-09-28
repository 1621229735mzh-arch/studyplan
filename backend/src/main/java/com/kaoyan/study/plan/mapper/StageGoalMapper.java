package com.kaoyan.study.plan.mapper;

import com.kaoyan.study.plan.entity.StageGoal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 阶段目标数据访问。 */
@Mapper
public interface StageGoalMapper {

    List<StageGoal> findAll(@Param("subjectId") Long subjectId, @Param("status") String status);

    StageGoal findById(@Param("id") Long id);

    int insert(StageGoal goal);

    int update(StageGoal goal);

    int deleteById(@Param("id") Long id);

    long countTasks(@Param("stageGoalId") Long stageGoalId);
}
