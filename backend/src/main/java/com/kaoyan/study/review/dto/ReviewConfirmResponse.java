package com.kaoyan.study.review.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 确认安排的结果。
 *
 * <p>超出额度的内容不会进入安排，而是留在待安排列表并说明原因。
 */
public record ReviewConfirmResponse(
        LocalDate date,
        Integer dailyReviewMinutes,
        int usedMinutes,
        List<ReviewItemResponse> scheduled,
        List<Rejected> rejected) {

    /** 未能安排的内容及原因。 */
    public record Rejected(Long reviewItemId, String title, String reason) {
    }
}
