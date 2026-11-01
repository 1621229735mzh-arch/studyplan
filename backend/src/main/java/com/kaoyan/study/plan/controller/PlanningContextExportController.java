package com.kaoyan.study.plan.controller;

import com.kaoyan.study.plan.dto.PlanningContextExport;
import com.kaoyan.study.plan.service.PlanningContextExportService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanningContextExportController {
    private final PlanningContextExportService service;
    public PlanningContextExportController(PlanningContextExportService service) { this.service=service; }

    @GetMapping("/api/plan/context-export")
    public ResponseEntity<PlanningContextExport> export() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.export());
    }
}
