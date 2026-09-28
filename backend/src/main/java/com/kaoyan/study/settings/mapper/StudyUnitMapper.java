package com.kaoyan.study.settings.mapper;

import com.kaoyan.study.settings.entity.StudyUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 计量单位数据访问。 */
@Mapper
public interface StudyUnitMapper {

    List<StudyUnit> findAll(@Param("enabledOnly") boolean enabledOnly);

    StudyUnit findById(@Param("id") Long id);

    StudyUnit findByName(@Param("name") String name);

    int insert(StudyUnit unit);

    int update(StudyUnit unit);

    int deleteById(@Param("id") Long id);

    long countUsages(@Param("unitId") Long unitId);
}
