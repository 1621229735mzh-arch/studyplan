package com.kaoyan.study.plan.service;

import com.kaoyan.study.plan.dto.PlanImportDocument;
import com.kaoyan.study.plan.mapper.TaskSequenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Service
public class TaskSequenceService {
    private final TaskSequenceMapper mapper;
    public TaskSequenceService(TaskSequenceMapper mapper) { this.mapper=mapper; }
    public TaskSequenceMapper.Definition definition(Long taskId) { return taskId==null ? null : mapper.definition(taskId); }
    public List<TaskSequenceMapper.Item> items(Long taskId) { return mapper.items(taskId); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void create(Long taskId, LocalDate start, LocalDate end, PlanImportDocument.Sequence sequence) {
        mapper.insert(taskId,start,end);
        for (int i=0;i<sequence.items().size();i++) {
            var item=sequence.items().get(i);
            mapper.insertItem(taskId,i,item.key(),item.title(),item.url());
        }
    }
}
