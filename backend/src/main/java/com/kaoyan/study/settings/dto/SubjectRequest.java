package com.kaoyan.study.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新建/修改科目的请求。 */
public record SubjectRequest(
        @NotBlank(message = "请填写科目名称")
        @Size(max = 64, message = "科目名称过长")
        String name,

        @Size(max = 32, message = "分类过长")
        String category,

        @Size(max = 16, message = "颜色值过长")
        String color,

        Integer sortOrder,

        Boolean enabled) {

    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }
}
