package com.kaoyan.study.plan.mapper;

import com.kaoyan.study.plan.service.TaskContinuationService.Definition;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskContinuationMapper {
    @Select("SELECT stream_id FROM task_continuation WHERE task_id=#{taskId}")
    Long streamId(Long taskId);
    @Select("SELECT id FROM study_continuation_stream WHERE id=#{id} FOR UPDATE")
    Long lock(Long id);
    @Insert("INSERT INTO study_continuation_stream(import_id,stream_key,title) VALUES(#{importId},#{key},#{title})")
    void insertStream(@Param("importId") Long importId,@Param("key") String key,@Param("title") String title);
    @Select("SELECT id FROM study_continuation_stream WHERE import_id=#{importId} AND stream_key=#{key}")
    Long importedStream(@Param("importId") Long importId,@Param("key") String key);
    @Insert("INSERT INTO task_continuation(task_id,stream_id,position_index) VALUES(#{taskId},#{streamId},#{position})")
    void insert(@Param("taskId") Long taskId,@Param("streamId") Long streamId,@Param("position") int position);
    @Select("""
        SELECT c.task_id,c.stream_id,s.title AS stream_title,c.position_index AS position,
               d.plan_date AS reference_date,d.estimated_minutes,a.description,t.subject_id,t.title,u.name AS unit_name,
               t.version AS task_version,d.version AS plan_version
        FROM task_continuation c JOIN study_continuation_stream s ON s.id=c.stream_id
        JOIN task t ON t.id=c.task_id JOIN study_unit u ON u.id=t.unit_id
        JOIN daily_plan_item d ON d.task_id=c.task_id JOIN task_activity a ON a.task_id=c.task_id
        ORDER BY c.stream_id,c.position_index
        """)
    List<Definition> all();
}
