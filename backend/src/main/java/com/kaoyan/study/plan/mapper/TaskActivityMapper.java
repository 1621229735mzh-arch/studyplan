package com.kaoyan.study.plan.mapper;
import com.kaoyan.study.plan.service.TaskActivityService.Activity;
import org.apache.ibatis.annotations.*;
@Mapper
public interface TaskActivityMapper {
    @Select("SELECT mode,description,flexible_minutes FROM task_activity WHERE task_id=#{taskId}")
    Activity find(Long taskId);
    @Insert("INSERT INTO task_activity(task_id,mode,description,flexible_minutes) VALUES(#{taskId},#{activity.mode},#{activity.description},#{activity.flexibleMinutes})")
    int insert(@Param("taskId") Long taskId,@Param("activity") Activity activity);
}
