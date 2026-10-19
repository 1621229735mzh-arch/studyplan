package com.kaoyan.study.learning.mapper;

import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.entity.StudyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 学习记录数据访问。所有查询都排除软删除记录。 */
@Mapper
public interface StudyRecordMapper {
    /** Batch retry validation includes revoked child tokens. */
    StudyRecord findByClientTokenIncludingDeleted(@Param("clientToken") String clientToken);
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM study_record WHERE task_id=#{taskId} AND record_date=#{date} AND deleted_at IS NULL AND activity_completed=TRUE")
    int completedActivities(@Param("taskId") Long taskId,@Param("date") LocalDate date);

    @org.apache.ibatis.annotations.Select("SELECT CONCAT(COUNT(*),':',COALESCE(MAX(id),0),':',COALESCE(SUM(version),0)) FROM study_record WHERE task_id=#{taskId} AND record_date=#{date}")
    String confirmationHistory(@Param("taskId") Long taskId,@Param("date") LocalDate date);

    List<StudyRecordView> findAll(@Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("taskId") Long taskId,
                                  @Param("subjectId") Long subjectId);

    @org.apache.ibatis.annotations.Select("SELECT CONCAT(COUNT(*),':',COALESCE(MAX(id),0),':',COALESCE(SUM(version),0)) FROM study_record WHERE task_id=#{taskId}")
    String sequenceHistory(@Param("taskId") Long taskId);

    StudyRecord findById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Select("SELECT id FROM study_record WHERE id=#{id} AND deleted_at IS NULL FOR UPDATE")
    Long lockRecord(@Param("id") Long id);

    StudyRecord findByClientToken(@Param("clientToken") String clientToken);

    int insert(StudyRecord record);

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(StudyRecord record);

    /** 软删除，同样做版本检查。 */
    int softDelete(@Param("id") Long id, @Param("version") Long version);

    StudyRecordView findView(@Param("id") Long id);
}
