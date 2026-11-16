package com.kaoyan.study.plan.mapper;

import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskSequenceMapper {
    record Definition(Long taskId, LocalDate startDate, LocalDate endDate, String mode, LocalDate dailyFrom) {
        public boolean contains(LocalDate date) {
            return !date.isBefore(startDate) && (endDate==null || !date.isAfter(endDate));
        }
        public boolean dailyOn(LocalDate date) { return dailyFrom!=null && !date.isBefore(dailyFrom); }
    }
    record DailyPractice(Long taskId, Long subjectId, String title, String unitName, Long version) {}
    record Item(Long id, Long taskId, Integer position, String itemKey, String title, String url) {}

    @Select("SELECT task_id, start_date, end_date, mode, daily_from FROM task_sequence WHERE task_id=#{taskId}")
    Definition definition(Long taskId);
    @Select("SELECT id, task_id, position, item_key, title, url FROM task_sequence_item WHERE task_id=#{taskId} ORDER BY position")
    List<Item> items(Long taskId);
    @Insert("INSERT INTO task_sequence(task_id,start_date,end_date) VALUES(#{taskId},#{start},#{end})")
    void insert(Long taskId, LocalDate start, LocalDate end);
    @Insert("INSERT INTO task_sequence_item(task_id,position,item_key,title,url) VALUES(#{taskId},#{position},#{key},#{title},#{url})")
    void insertItem(Long taskId, int position, String key, String title, String url);

    @Update("UPDATE task_sequence SET mode='SELECT_BY_NUMBER' WHERE task_id=#{taskId}")
    void selectByNumber(Long taskId);

    @Update("UPDATE task_sequence SET end_date=NULL,daily_from=#{from} WHERE task_id=#{taskId} AND mode='SELECT_BY_NUMBER' AND daily_from IS NULL")
    int startDailyPractice(Long taskId, LocalDate from);

    @Select("""
        SELECT t.id AS task_id,t.subject_id,t.title,u.name AS unit_name,t.version
        FROM task_sequence s JOIN task t ON t.id=s.task_id JOIN study_unit u ON u.id=t.unit_id
        WHERE s.daily_from<=#{date} AND s.end_date IS NULL AND t.status='ACTIVE' ORDER BY t.id
        """)
    List<DailyPractice> dailyPractice(LocalDate date);
}
