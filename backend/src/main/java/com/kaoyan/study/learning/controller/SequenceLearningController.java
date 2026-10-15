package com.kaoyan.study.learning.controller;

import com.kaoyan.study.learning.service.SequenceLearningService;
import com.kaoyan.study.plan.service.TaskService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/learning/sequences")
public class SequenceLearningController {
    private final SequenceLearningService sequences;
    private final TaskService tasks;
    public SequenceLearningController(SequenceLearningService sequences, TaskService tasks) { this.sequences=sequences; this.tasks=tasks; }
    @GetMapping("/{taskId}")
    public SequenceLearningService.State get(@PathVariable Long taskId, @RequestParam LocalDate date) {
        tasks.get(taskId);
        return sequences.state(taskId,date);
    }
}
