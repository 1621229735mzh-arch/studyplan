package com.kaoyan.study.plan.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskSequenceMapper {
    record Definition(Long taskId, LocalDate startDate, LocalDate endDate, String mode) {}
    record Item(Long id, Long taskId, Integer position, String itemKey, String title, String url) {}

    @Select("SELECT task_id, start_date, end_date, mode FROM task_sequence WHERE task_id=#{taskId}")
    Definition definition(Long taskId);
    @Select("SELECT id, task_id, position, item_key, title, url FROM task_sequence_item WHERE task_id=#{taskId} ORDER BY position")
    List<Item> items(Long taskId);
    @Insert("INSERT INTO task_sequence(task_id,start_date,end_date) VALUES(#{taskId},#{start},#{end})")
    void insert(Long taskId, LocalDate start, LocalDate end);
    @Insert("INSERT INTO task_sequence_item(task_id,position,item_key,title,url) VALUES(#{taskId},#{position},#{key},#{title},#{url})")
    void insertItem(Long taskId, int position, String key, String title, String url);

    @Update("UPDATE task_sequence SET mode='SELECT_BY_NUMBER' WHERE task_id=#{taskId}")
    void selectByNumber(Long taskId);
}
