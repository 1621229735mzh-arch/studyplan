package com.kaoyan.study.progress.mapper;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 本模块写目录及归属；跨模块关联仅用于只读统计。 */
@Mapper
public interface CatalogMapper {
    record CourseRow(Long id, String courseKey, String direction, String branch, String title, Long subjectId, Long unitId) {}
    record NodeRow(Long id, Long courseId, String nodeKey, String parentKey, String title, BigDecimal totalAmount, Integer sortOrder) {}
    record TaskLink(Long taskId, Long nodeId) {}
    record RecordLink(Long recordId, Long nodeId, Long sourceTaskId) {}
    record CountedTask(Long id, String title, Long subjectId, Long unitId, String mode) {}
    record CompletedProblem(Long recordId, Integer problemNumber) {}
    record PreviewRow(String token, Long revision, String payload, String contextHash, LocalDateTime expiresAt, Integer applied) {}

    @Select("SELECT version FROM progress_catalog_revision WHERE id=1") long revision();
    @Select("SELECT version FROM progress_catalog_revision WHERE id=1 FOR UPDATE") long lock();
    @Update("UPDATE progress_catalog_revision SET version=version+1 WHERE id=1") void touch();
    @Select("SELECT id,course_key,direction,branch,title,subject_id,unit_id FROM progress_course ORDER BY id") List<CourseRow> courses();
    @Select("SELECT id,course_id,node_key,parent_key,title,total_amount,sort_order FROM progress_catalog_node ORDER BY sort_order,id") List<NodeRow> nodes();
    @Select("SELECT task_id,node_id FROM progress_task_link ORDER BY task_id") List<TaskLink> taskLinks();
    @Select("SELECT record_id,node_id,source_task_id FROM progress_record_link ORDER BY record_id") List<RecordLink> recordLinks();
    @Select("SELECT t.id,t.title,t.subject_id,t.unit_id,a.mode FROM task t JOIN task_activity a ON a.task_id=t.id WHERE a.mode IN ('COUNT_STUDY','COUNT_REVIEW') ORDER BY a.mode DESC,t.id")
    List<CountedTask> countedTasks();
    @Select("""
            SELECT r.id AS record_id, CAST(i.item_key AS UNSIGNED) AS problem_number
            FROM study_record r
            JOIN task_sequence_item i ON i.id=r.sequence_item_id AND i.task_id=r.task_id
            JOIN task_sequence s ON s.task_id=r.task_id AND s.mode='SELECT_BY_NUMBER'
            WHERE r.deleted_at IS NULL AND r.amount>0
              AND i.item_key REGEXP '^[1-9][0-9]{0,9}$'
              AND CAST(i.item_key AS UNSIGNED)<=2147483647
            """)
    List<CompletedProblem> completedProblems();
    @Insert("INSERT INTO progress_course(course_key,direction,branch,title,subject_id,unit_id) VALUES(#{key},#{direction},#{branch},#{title},#{subjectId},#{unitId})")
    void insertCourse(@Param("key") String key,@Param("direction") String direction,@Param("branch") String branch,@Param("title") String title,@Param("subjectId") Long subjectId,@Param("unitId") Long unitId);
    @Update("UPDATE progress_course SET direction=#{direction},branch=#{branch},title=#{title} WHERE id=#{id}")
    void updateCourse(@Param("id") Long id,@Param("direction") String direction,@Param("branch") String branch,@Param("title") String title);
    @Insert("INSERT INTO progress_catalog_node(course_id,node_key,parent_key,title,total_amount,sort_order) VALUES(#{courseId},#{key},#{parentKey},#{title},#{amount},#{sortOrder}) ON DUPLICATE KEY UPDATE parent_key=VALUES(parent_key),title=VALUES(title),total_amount=VALUES(total_amount),sort_order=VALUES(sort_order)")
    void upsertNode(@Param("courseId") Long courseId,@Param("key") String key,@Param("parentKey") String parentKey,@Param("title") String title,@Param("amount") BigDecimal amount,@Param("sortOrder") int sortOrder);
    @Delete("DELETE FROM progress_task_link WHERE node_id=#{nodeId}") void clearTasks(Long nodeId);
    @Insert("INSERT INTO progress_task_link(task_id,node_id) VALUES(#{taskId},#{nodeId})") void linkTask(@Param("taskId") Long taskId,@Param("nodeId") Long nodeId);
    @Insert("INSERT INTO progress_record_link(record_id,node_id,source_task_id) VALUES(#{recordId},#{nodeId},#{taskId}) ON DUPLICATE KEY UPDATE node_id=VALUES(node_id),source_task_id=VALUES(source_task_id)")
    void assign(@Param("recordId") Long recordId,@Param("nodeId") Long nodeId,@Param("taskId") Long taskId);
    @Delete("DELETE FROM progress_record_link WHERE record_id=#{recordId}") void unassign(Long recordId);
    @Insert("INSERT INTO progress_catalog_preview(token,revision,payload,context_hash,expires_at) VALUES(#{token},#{revision},#{payload},#{contextHash},#{expiresAt})")
    void insertPreview(@Param("token") String token,@Param("revision") long revision,@Param("payload") String payload,@Param("contextHash") String contextHash,@Param("expiresAt") LocalDateTime expiresAt);
    @Select("SELECT token,revision,payload,context_hash,expires_at,applied FROM progress_catalog_preview WHERE token=#{token} FOR UPDATE") PreviewRow preview(String token);
    @Update("UPDATE progress_catalog_preview SET applied=1 WHERE token=#{token}") void applied(String token);
    @Delete("DELETE FROM progress_catalog_preview WHERE expires_at < CURRENT_TIMESTAMP AND applied=0") void clearExpired();
}
