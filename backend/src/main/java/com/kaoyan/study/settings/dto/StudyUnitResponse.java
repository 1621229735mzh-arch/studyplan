package com.kaoyan.study.settings.dto;

import com.kaoyan.study.settings.entity.StudyUnit;

/** 计量单位响应。 */
public record StudyUnitResponse(Long id, String name, int sortOrder, boolean enabled) {

    public static StudyUnitResponse from(StudyUnit unit) {
        return new StudyUnitResponse(unit.getId(), unit.getName(), unit.getSortOrder(), unit.isEnabled());
    }
}
