package com.kaoyan.study.learning.controller;

import com.kaoyan.study.learning.dto.RecordArchive;
import com.kaoyan.study.learning.service.RecordArchiveService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/learning/archive")
public class RecordArchiveController {
    private final RecordArchiveService service;
    public RecordArchiveController(RecordArchiveService service) {this.service=service;}
    @GetMapping public RecordArchive archive(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) Long subjectId,@RequestParam(required=false) Long taskId) {
        return service.archive(from,to,subjectId,taskId);
    }
}
