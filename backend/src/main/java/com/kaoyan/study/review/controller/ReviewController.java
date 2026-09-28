package com.kaoyan.study.review.controller;

import com.kaoyan.study.review.dto.ReviewConfirmRequest;
import com.kaoyan.study.review.dto.ReviewConfirmResponse;
import com.kaoyan.study.review.dto.ReviewItemCreateRequest;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.dto.ReviewRecordCreateRequest;
import com.kaoyan.study.review.dto.ReviewRecordResponse;
import com.kaoyan.study.review.dto.ReviewSuggestionResponse;
import com.kaoyan.study.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 复习：加入复习、掌握反馈、日期建议与确认安排。
 *
 * <p>建议只是建议：未经确认不会进入某一天的安排，超额内容留在待安排列表。
 */
@RestController
@RequestMapping("/api/review")
@Tag(name = "复习")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/items")
    @Operation(summary = "复习项列表")
    public List<ReviewItemResponse> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) Long subjectId) {
        return reviewService.list(status, subjectId).stream().map(ReviewItemResponse::from).toList();
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "复习项详情")
    public ReviewItemResponse get(@PathVariable Long id) {
        return ReviewItemResponse.from(reviewService.get(id));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "把已学内容加入复习")
    public ReviewItemResponse addToReview(@Valid @RequestBody ReviewItemCreateRequest request) {
        return ReviewItemResponse.from(reviewService.addToReview(request));
    }

    @PostMapping("/items/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "归档复习项（保留历史复习记录）")
    public void archive(@PathVariable Long id, @RequestParam Long version) {
        reviewService.archive(id, version);
    }

    @GetMapping("/items/{id}/records")
    @Operation(summary = "复习记录")
    public List<ReviewRecordResponse> records(@PathVariable Long id) {
        return reviewService.records(id).stream()
                .map(record -> ReviewRecordResponse.from(record, record.getNextReviewDate() != null))
                .toList();
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "提交复习反馈（不会/模糊/掌握），据此建议下次日期")
    public ReviewRecordResponse record(@Valid @RequestBody ReviewRecordCreateRequest request) {
        return reviewService.recordReview(request);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "某天的复习建议（含额度与预计用时缺口）")
    public ReviewSuggestionResponse suggestions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reviewService.suggestions(date == null ? LocalDate.now() : date);
    }

    @PostMapping("/suggestions/confirm")
    @Operation(summary = "确认把建议排入某一天（超出额度的留在待安排列表）")
    public ReviewConfirmResponse confirm(@Valid @RequestBody ReviewConfirmRequest request) {
        return reviewService.confirm(request);
    }
}
