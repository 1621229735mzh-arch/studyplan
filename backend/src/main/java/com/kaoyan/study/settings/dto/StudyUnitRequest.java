package com.kaoyan.study.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 新建/修改计量单位的请求。 */
public record StudyUnitRequest(
        @NotBlank(message = "请填写单位名称")
        @Size(max = 32, message = "单位名称过长")
        String name,

        Integer sortOrder,

        Boolean enabled) {

    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public boolean enabledOrDefault() {
        return enabled == null || enabled;
    }
}
