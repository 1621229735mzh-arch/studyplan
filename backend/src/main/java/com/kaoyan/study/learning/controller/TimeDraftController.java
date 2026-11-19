package com.kaoyan.study.learning.controller;

import java.time.LocalDateTime;
import com.kaoyan.study.learning.dto.TimeDraft;
import com.kaoyan.study.learning.service.TimeDraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning/time-drafts/{context}")
public class TimeDraftController {
    private final TimeDraftService drafts;

    public TimeDraftController(TimeDraftService drafts) {
        this.drafts = drafts;
    }

    public record Change(@NotNull LocalDateTime startedAt, String revision) {}
    public record Result(TimeDraft draft) {}
    private static final String CONTEXT = "(task|subject|record)-[1-9][0-9]{0,18}";

    @GetMapping
    public Result get(@PathVariable @Pattern(regexp = CONTEXT) String context) {
        return new Result(drafts.get(context));
    }

    @PutMapping
    public TimeDraft save(@PathVariable @Pattern(regexp = CONTEXT) String context, @Valid @RequestBody Change change) {
        return drafts.save(context, change.startedAt(), change.revision());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable @Pattern(regexp = CONTEXT) String context, @RequestParam String revision) {
        drafts.remove(context, revision);
    }
}
