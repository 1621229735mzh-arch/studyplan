package com.kaoyan.study.target.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 只修改候选目标状态的请求。
 *
 * <p>带 {@code version}：改状态同样是写操作，需要和其他编辑一样检查数据版本。
 */
public record TargetStatusRequest(
        @NotBlank(message = "请选择目标状态")
        String status,

        @NotNull(message = "缺少数据版本，无法安全更新")
        Long version) {
}
