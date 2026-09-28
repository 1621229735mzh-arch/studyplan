package com.kaoyan.study.settings.dto;

import com.kaoyan.study.settings.entity.Subject;

/** 科目响应。 */
public record SubjectResponse(Long id, String name, String category, String color, int sortOrder, boolean enabled) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getCategory(),
                subject.getColor(), subject.getSortOrder(), subject.isEnabled());
    }
}
