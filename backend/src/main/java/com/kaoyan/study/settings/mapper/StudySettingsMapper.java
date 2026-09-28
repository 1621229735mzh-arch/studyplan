package com.kaoyan.study.settings.mapper;

import com.kaoyan.study.settings.entity.StudySettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 学习设置数据访问（单行，id 固定为 1）。 */
@Mapper
public interface StudySettingsMapper {

    StudySettings find();

    /** 不存在时插入默认行，存在时不改动。 */
    int insertDefaultIfAbsent();

    /** 按 id + version 更新，返回受影响行数；0 表示版本已过期。 */
    int update(StudySettings settings);

    StudySettings findForUpdate();
}
