package com.kaoyan.study.target.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新建/修改候选目标的请求。
 *
 * <p>{@code version} 必填：新建时前端传 0，服务端只在校验更新时使用，用于检测
 * 数据是否已被其他设备修改。
 *
 * <p>链接随目标整体提交：提交的列表就是目标现有的全部链接，服务端按提交顺序替换。
 */
public record TargetRequest(
        @NotBlank(message = "请填写院校名称")
        @Size(max = 100, message = "院校名称过长")
        String schoolName,

        @Size(max = 100, message = "专业名称过长")
        String majorName,

        @Size(max = 1000, message = "备注过长")
        String note,

        /** CANDIDATE / CHOSEN / DROPPED；为空表示新建用 CANDIDATE、修改保持原状态。 */
        String status,

        Integer sortOrder,

        /** 链接元素逐个校验：地址必填、长度受限，见 {@link LinkRequest}。 */
        List<@Valid LinkRequest> links,

        @NotNull(message = "缺少数据版本，无法安全更新")
        Long version) {

    public int sortOrderOrDefault() {
        return sortOrder == null ? 0 : sortOrder;
    }

    /** 参考链接。 */
    public record LinkRequest(
            @NotBlank(message = "请填写链接地址")
            @Size(max = 500, message = "链接地址过长")
            String url,

            @Size(max = 100, message = "链接名称过长")
            String label) {
    }
}
