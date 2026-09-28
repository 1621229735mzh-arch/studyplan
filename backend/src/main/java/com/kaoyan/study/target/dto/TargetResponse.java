package com.kaoyan.study.target.dto;

import com.kaoyan.study.target.entity.TargetLink;
import com.kaoyan.study.target.entity.TargetSchool;

import java.time.LocalDateTime;
import java.util.List;

/** 候选目标响应，含其参考链接。 */
public record TargetResponse(
        Long id,
        String schoolName,
        String majorName,
        String note,
        String status,
        int sortOrder,
        List<LinkResponse> links,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {

    public static TargetResponse from(TargetSchool target, List<TargetLink> links) {
        return new TargetResponse(target.getId(), target.getSchoolName(), target.getMajorName(), target.getNote(),
                target.getStatus(), target.getSortOrder(),
                links == null ? List.of() : links.stream().map(LinkResponse::from).toList(),
                target.getCreatedAt(), target.getUpdatedAt(), target.getVersion());
    }

    /** 参考链接。 */
    public record LinkResponse(Long id, String url, String label, int sortOrder) {

        public static LinkResponse from(TargetLink link) {
            return new LinkResponse(link.getId(), link.getUrl(), link.getLabel(), link.getSortOrder());
        }
    }
}
