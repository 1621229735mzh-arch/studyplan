package com.kaoyan.study.learning.mapper;

import com.kaoyan.study.learning.dto.TimeDraft;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TimeDraftMapper {
    TimeDraft find(String username, String context);
    int insert(String username, String context, TimeDraft draft);
    int replace(String username, String context, String expected, TimeDraft draft);
    int remove(String username, String context, String expected);
}
