package com.kaoyan.study.review.controller;

import com.kaoyan.study.review.dto.AutoReviewContracts.*;
import com.kaoyan.study.review.service.AutoReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/review/automatic/books")
public class AutoReviewController {
    private final AutoReviewService service;
    public AutoReviewController(AutoReviewService service) { this.service=service; }
    @GetMapping public List<Book> books() { return service.books(); }
    @GetMapping("/segments") public List<Segment> segments() { return service.segments(); }
    @PostMapping("/{streamId}") public Book policy(@PathVariable Long streamId,@Valid @RequestBody PolicyRequest request) {
        return service.setPolicy(streamId,request);
    }
}
