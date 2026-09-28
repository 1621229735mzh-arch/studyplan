package com.kaoyan.study.plan.mapper;

import com.kaoyan.study.plan.dto.TaskProgressView;
import com.kaoyan.study.plan.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务数据访问。
 *
 * <p>这里的“剩余工作量”查询读取学习记录，属于文档允许的只读关联查询：
 * 任务的计划量与实际完成量必须来自同一事实，不能各自维护一份。
 */
@Mapper
public interface TaskMapper {

    List<Task> findAll(@Param("subjectId") Long subjectId,
                       @Param("stageGoalId") Long stageGoalId,
                       @Param("status") String status);

    Task findById(@Param("id") Long id);

    Task findByIdForUpdate(@Param("id") Long id);

    int insert(Task task);

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(Task task);

    int deleteById(@Param("id") Long id);

    long countRecords(@Param("taskId") Long taskId);

    TaskProgressView findProgress(@Param("taskId") Long taskId);
}
