package com.kaoyan.study.plan.service;

import com.kaoyan.study.plan.dto.PlanImportDocument;
import com.kaoyan.study.plan.mapper.TaskContinuationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;
import java.util.*;

/** Only ordered planning metadata. No completion state or cursor is stored here. */
@Service
public class TaskContinuationService {
    public record Definition(Long taskId,Long streamId,String streamTitle,Integer position,
            LocalDate referenceDate,Integer estimatedMinutes,String description,Long subjectId,String title,
            String unitName,Long taskVersion,Long planVersion) {}
    private final TaskContinuationMapper mapper;
    public TaskContinuationService(TaskContinuationMapper mapper) { this.mapper=mapper; }
    public List<Definition> all() { return mapper.all(); }
    public Long streamId(Long taskId) { return taskId==null ? null : mapper.streamId(taskId); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void lockForTasks(Long... taskIds) {
        Arrays.stream(taskIds).filter(Objects::nonNull).map(mapper::streamId).filter(Objects::nonNull)
                .distinct().sorted().forEach(mapper::lock);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void create(Long importId,List<PlanImportDocument.Task> tasks,Map<String,Long> ids) {
        var streams=new HashMap<String,Long>();
        for (var task:tasks) if (task.continuation()!=null) {
            var c=task.continuation();
            Long stream=streams.get(c.streamKey());
            if (stream==null) {
                mapper.insertStream(importId,c.streamKey(),c.title());
                stream=mapper.importedStream(importId,c.streamKey()); streams.put(c.streamKey(),stream);
            }
            mapper.insert(ids.get(task.key()),stream,c.position());
        }
    }
}
