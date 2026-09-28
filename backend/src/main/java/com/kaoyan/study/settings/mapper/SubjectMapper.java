package com.kaoyan.study.settings.mapper;

import com.kaoyan.study.settings.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 科目数据访问。SQL 见 {@code resources/mapper/settings/SubjectMapper.xml}。 */
@Mapper
public interface SubjectMapper {

    List<Subject> findAll(@Param("enabledOnly") boolean enabledOnly);

    Subject findById(@Param("id") Long id);

    Subject findByName(@Param("name") String name);

    int insert(Subject subject);

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(Subject subject);

    int deleteById(@Param("id") Long id);

    long countUsages(@Param("subjectId") Long subjectId);
}
