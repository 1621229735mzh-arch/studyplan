package com.kaoyan.study.progress.controller;

import com.kaoyan.study.progress.dto.CatalogContracts.*;
import com.kaoyan.study.progress.service.CatalogService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/progress/catalog")
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) {this.service=service;}
    @GetMapping public Library library() {return service.library();}
    @GetMapping("/candidates") public List<TaskCandidate> candidates() {return service.candidates();}
    @PostMapping("/preview") public Preview preview(@RequestBody SourceRequest request) {return service.preview(request.document());}
    @PostMapping("/confirm") public Library confirm(@RequestBody ConfirmRequest request) {return service.confirm(request.previewToken());}
    @PutMapping("/records/{id}") public Library assign(@PathVariable Long id,@RequestBody AssignRequest request) {return service.assign(id,request);}
    @PutMapping("/nodes/{id}/tasks") public Library tasks(@PathVariable Long id,@RequestBody TaskLinkRequest request) {return service.linkTasks(id,request);}
}
