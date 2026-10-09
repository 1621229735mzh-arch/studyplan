package com.kaoyan.study.plan.controller;
import com.kaoyan.study.plan.dto.*;
import com.kaoyan.study.plan.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
public class PlanWorkspaceController {
    private final PlanWorkspaceService workspace;
    private final PlanImportService imports;
    public PlanWorkspaceController(PlanWorkspaceService workspace,PlanImportService imports) {
        this.workspace=workspace; this.imports=imports;
    }
    @GetMapping("/workspace")
    public PlanWorkspaceResponse read(
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required=false) Long subjectId) { return workspace.read(startDate,endDate,subjectId); }
    @PostMapping("/days/{date}/quick-items")
    public List<DailyPlanItemView> quick(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                       @Valid @RequestBody QuickDayRequest request) { return workspace.quickAdd(date,request); }
    @PutMapping("/days/{date}/order")
    public List<DailyPlanItemView> order(@PathVariable @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                       @Valid @RequestBody ReorderRequest request) { return workspace.reorder(date,request); }
    @PostMapping("/imports/preview")
    public PlanImportPreview preview(@Valid @RequestBody PlanImportRequest request) { return imports.preview(request.document()); }
    @PostMapping("/imports/confirm")
    public PlanImportPreview confirm(@Valid @RequestBody PlanImportRequest request) { return imports.confirm(request); }
}
