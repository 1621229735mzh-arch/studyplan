package com.kaoyan.study.review.mapper;

import com.kaoyan.study.review.dto.AutoReviewContracts.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AutoReviewMapper {
    record QueueRow(Long id, Long subjectId, String title, Long streamId, String bookTitle,
            LocalDate dueDate, Integer intervalDays, String lastResult, int repetitions,
            BigDecimal completionPercent, Integer estimatedMinutes, int actualMinutes,
            int missingDurations, Long version, boolean eligible, boolean completed, String dateSource) {}

    @Select("""
        SELECT i.id,i.subject_id,i.title,f.stream_id,s.title AS book_title,i.scheduled_date AS due_date,
               i.interval_days,i.last_result,f.repetitions,
               LEAST(COALESCE(r.percent_done,0),100) AS completion_percent,
               COALESCE(r.estimate,i.estimated_minutes) AS estimated_minutes,
               COALESCE(r.minutes,0) AS actual_minutes,COALESCE(r.missing_durations,0) AS missing_durations,i.version,
               (p.enabled AND f.source_valid AND NOT f.paused AND t.status='ACTIVE'
                AND i.status='SCHEDULED' AND i.scheduled_date<=#{date}) AS eligible,
               COALESCE(r.completed,FALSE) AS completed,i.date_source
        FROM review_item i JOIN review_fsrs_state f ON f.review_item_id=i.id
        JOIN review_auto_policy p ON p.stream_id=f.stream_id
        JOIN study_continuation_stream s ON s.id=f.stream_id JOIN task t ON t.id=i.task_id
        LEFT JOIN (SELECT review_item_id,SUM(progress_delta) AS percent_done,SUM(duration_minutes) AS minutes,
                   SUM(duration_minutes IS NULL) AS missing_durations,MAX(estimated_minutes) AS estimate,
                   MAX(result IS NOT NULL) AS completed
                   FROM review_record WHERE review_date=#{date} GROUP BY review_item_id) r ON r.review_item_id=i.id
        WHERE (p.enabled AND f.source_valid AND NOT f.paused AND t.status='ACTIVE'
               AND i.status='SCHEDULED' AND i.scheduled_date<=#{date}) OR r.review_item_id IS NOT NULL
        ORDER BY f.stream_id,i.id
        """)
    List<QueueRow> queue(LocalDate date);
    @Select("""
        SELECT s.id AS stream_id, MIN(t.subject_id) AS subject_id, s.title,
               COALESCE(p.enabled, FALSE) AS enabled, COALESCE(p.version,0) AS version,
               COUNT(DISTINCT c.task_id) AS segment_count, COUNT(DISTINCT f.review_item_id) AS review_count,
               COUNT(DISTINCT CASE WHEN i.status='SCHEDULED' AND i.scheduled_date < #{today} THEN f.review_item_id END) AS overdue_count,
               COUNT(DISTINCT CASE WHEN NOT f.source_valid OR f.paused THEN f.review_item_id END) AS suspended_count
        FROM study_continuation_stream s JOIN task_continuation c ON c.stream_id=s.id
        JOIN task t ON t.id=c.task_id LEFT JOIN review_auto_policy p ON p.stream_id=s.id
        LEFT JOIN review_item i ON i.task_id=c.task_id LEFT JOIN review_fsrs_state f ON f.review_item_id=i.id
        GROUP BY s.id,s.title,p.enabled,p.version ORDER BY s.id
        """)
    List<Book> books(LocalDate today);
    @Select("SELECT task_id FROM task_continuation WHERE stream_id=#{streamId} ORDER BY task_id")
    List<Long> taskIds(Long streamId);
    @Select("SELECT enabled FROM review_auto_policy WHERE stream_id=#{streamId}")
    Boolean enabled(Long streamId);
    @Insert("INSERT IGNORE INTO review_auto_policy(stream_id) VALUES(#{streamId})")
    void ensurePolicy(Long streamId);
    @Update("UPDATE review_auto_policy SET enabled=#{enabled},version=version+1 WHERE stream_id=#{streamId} AND version=#{version}")
    int policy(@Param("streamId") Long streamId, @Param("enabled") boolean enabled, @Param("version") Long version);
    @Select("SELECT * FROM review_fsrs_state WHERE review_item_id=#{id}")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    State state(Long id);
    @Select("""
        SELECT f.review_item_id,f.stream_id,i.title,f.completed_on,f.source_valid,f.paused,p.enabled,
               i.status,i.scheduled_date,i.next_review_date,f.repetitions,i.version,i.date_source
        FROM review_fsrs_state f JOIN review_item i ON i.id=f.review_item_id
        JOIN review_auto_policy p ON p.stream_id=f.stream_id
        JOIN task_continuation c ON c.task_id=i.task_id
        ORDER BY f.stream_id,c.position_index
        """)
    List<Segment> segments();
    @Insert("INSERT INTO review_fsrs_state(review_item_id,stream_id,completed_on) VALUES(#{itemId},#{streamId},#{completedOn})")
    void insert(@Param("itemId") Long itemId,@Param("streamId") Long streamId,@Param("completedOn") LocalDate completedOn);
    @Update("UPDATE review_fsrs_state SET completed_on=COALESCE(#{date},completed_on),source_valid=#{valid} WHERE review_item_id=#{id}")
    void source(@Param("id") Long id,@Param("date") LocalDate date,@Param("valid") boolean valid);
    @Update("UPDATE review_fsrs_state SET paused=#{paused} WHERE review_item_id=#{id}")
    void pause(@Param("id") Long id,@Param("paused") boolean paused);
    @Update("""
        UPDATE review_fsrs_state SET stability=#{stability},difficulty=#{difficulty},last_review_date=#{date},
        repetitions=repetitions+1 WHERE review_item_id=#{id}
        """)
    void memory(@Param("id") Long id,@Param("stability") double stability,@Param("difficulty") double difficulty,@Param("date") LocalDate date);
    @Update("""
        UPDATE review_fsrs_state SET stability=#{stability},difficulty=#{difficulty},last_review_date=#{date},
        repetitions=#{repetitions} WHERE review_item_id=#{id}
        """)
    void replaceMemory(@Param("id") Long id,@Param("stability") double stability,@Param("difficulty") double difficulty,
                       @Param("date") LocalDate date,@Param("repetitions") int repetitions);
}
