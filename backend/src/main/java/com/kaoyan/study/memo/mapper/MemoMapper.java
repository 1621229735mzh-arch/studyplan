package com.kaoyan.study.memo.mapper;

import com.kaoyan.study.memo.entity.Memo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 备忘录数据访问。SQL 见 {@code resources/mapper/memo/MemoMapper.xml}。 */
@Mapper
public interface MemoMapper {

    /** 三个筛选条件都可为空：为空表示该条件不参与过滤。 */
    List<Memo> findAll(@Param("status") String status,
                       @Param("subjectId") Long subjectId,
                       @Param("keyword") String keyword);

    Memo findById(@Param("id") Long id);

    Memo findByIdForUpdate(@Param("id") Long id);

    /** 到期提醒候选：仍为 OPEN、到期日不晚于给定日期，且本次提醒尚未被忽略。 */
    List<Memo> findDue(@Param("date") LocalDate date);

    int insert(Memo memo);

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(Memo memo);

    int deleteById(@Param("id") Long id);

    /** 标记本次到期提醒已忽略；不改内容、状态与版本。 */
    int dismissReminder(@Param("id") Long id);

    /** 记录转成的任务；状态保持不变，备忘录原文保留为任务来源。 */
    int updateConvertedTask(@Param("id") Long id, @Param("taskId") Long taskId);
}
