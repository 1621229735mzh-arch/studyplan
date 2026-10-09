package com.kaoyan.study.plan.mapper;

import com.kaoyan.study.plan.dto.ScheduleEntryView;
import com.kaoyan.study.plan.dto.PlanWorkspaceResponse.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkspaceMapper {
    @Select("SELECT version FROM plan_workspace_revision WHERE id=1")
    long revision();
    @Select("SELECT version FROM plan_workspace_revision WHERE id=1 FOR UPDATE")
    long lock();
    @Select("SELECT COALESCE(MAX(sort_order),-1)+1 FROM daily_plan_item WHERE plan_date=#{date}")
    int nextOrder(LocalDate date);
    @Update("UPDATE daily_plan_item SET sort_order=#{order} WHERE id=#{id}")
    int initializeOrder(@Param("id") Long id, @Param("order") int order);
    @Update("UPDATE plan_workspace_revision SET version=version+1 WHERE id=1")
    int touch();
    @Select("SELECT request_hash FROM plan_quick_submission WHERE client_token=#{token}")
    String quickHash(String token);
    @Insert("INSERT INTO plan_quick_submission(client_token,request_hash,plan_date) VALUES(#{token},#{hash},#{date})")
    int rememberQuick(@Param("token") String token,@Param("hash") String hash,@Param("date") LocalDate date);

    List<ScheduleEntryView> entries(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                   @Param("subjectId") Long subjectId);
    @Select("SELECT period_type, start_date, title, description FROM plan_period_outline ORDER BY start_date,id")
    List<Outline> outlines();
    @Select("SELECT plan_key, title, start_date, end_date FROM plan_import ORDER BY start_date,id")
    List<ImportedPlan> imports();
    @Select("SELECT COUNT(*) FROM plan_import WHERE plan_key=#{key} OR content_hash=#{hash}")
    long imported(@Param("key") String key, @Param("hash") String hash);
    @Insert("INSERT INTO plan_import(plan_key,content_hash,title,start_date,end_date) VALUES(#{key},#{hash},#{title},#{start},#{end})")
    int insertImport(@Param("key") String key, @Param("hash") String hash, @Param("title") String title,
                     @Param("start") LocalDate start, @Param("end") LocalDate end);
    @Select("SELECT id FROM plan_import WHERE plan_key=#{key}")
    Long importId(String key);
    @Insert("INSERT INTO plan_period_outline(import_id,period_type,start_date,title,description) VALUES(#{id},#{type},#{start},#{title},#{description})")
    int insertOutline(@Param("id") Long id, @Param("type") String type, @Param("start") LocalDate start,
                      @Param("title") String title, @Param("description") String description);
    @Update("UPDATE daily_plan_item SET sort_order=#{order},version=version+1 WHERE id=#{id} AND version=#{version} AND plan_date=#{date}")
    int reorder(@Param("date") LocalDate date, @Param("id") Long id, @Param("version") Long version,
                @Param("order") int order);
}
