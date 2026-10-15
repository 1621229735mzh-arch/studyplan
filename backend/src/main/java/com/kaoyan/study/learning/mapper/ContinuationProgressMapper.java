package com.kaoyan.study.learning.mapper;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ContinuationProgressMapper {
    record Row(Long id,Long taskId,LocalDate recordDate,BigDecimal delta,Long version,Boolean deleted) {}
    // Tombstones participate in the revision: deleting and recreating a record never restores an old token.
    @Select("""
        SELECT r.id,r.task_id,r.record_date,r.continuation_delta AS delta,r.version,
               (r.deleted_at IS NOT NULL) AS deleted
        FROM study_record r JOIN task_continuation c ON c.task_id=r.task_id
        WHERE c.stream_id=#{streamId} ORDER BY r.id
        """)
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    List<Row> history(Long streamId);
    @Select("SELECT COALESCE(SUM(continuation_delta),0) FROM study_record WHERE task_id=#{taskId} AND deleted_at IS NULL")
    @Options(useCache=false,flushCache=Options.FlushCachePolicy.TRUE)
    BigDecimal total(Long taskId);
}
