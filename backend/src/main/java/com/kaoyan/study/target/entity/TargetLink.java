package com.kaoyan.study.target.entity;

import java.time.LocalDateTime;

/**
 * 候选目标的参考链接。
 *
 * <p>随目标整体保存：链接没有独立身份，编辑目标时按提交顺序整体替换。
 * 不直接用于接口响应：接口只暴露 {@code target.dto} 中的类型。
 */
public class TargetLink {

    private Long id;
    private Long targetId;
    private String url;
    private String label;
    private int sortOrder;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
