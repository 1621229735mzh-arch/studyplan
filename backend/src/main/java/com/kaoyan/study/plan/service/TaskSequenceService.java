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
        if ("SELECT_BY_NUMBER".equals(sequence.mode())) mapper.selectByNumber(taskId);
        for (int i=0;i<sequence.items().size();i++) {
            var item=sequence.items().get(i);
            mapper.insertItem(taskId,i,item.key(),item.title(),item.url());
        }
    }

    public boolean selectsNumber(Long taskId) {
        var definition=definition(taskId);
        return definition!=null && "SELECT_BY_NUMBER".equals(definition.mode());
    }

    @Transactional(propagation=Propagation.MANDATORY)
    public void selectByNumber(Long taskId) { mapper.selectByNumber(taskId); }

    /** Caller holds the task lock. This directory only stores identities, never completion. */
    @Transactional(propagation=Propagation.MANDATORY)
    public TaskSequenceMapper.Item resolveNumber(Long taskId, Integer number) {
        if (!selectsNumber(taskId) || number==null || number<1)
            throw new com.kaoyan.study.common.exception.BusinessException("PROBLEM_NUMBER_REQUIRED","请填写题号（正整数）");
        var items=mapper.items(taskId);
        String key=number.toString();
        var existing=items.stream().filter(i->i.itemKey().equals(key)).findFirst();
        if (existing.isPresent()) return existing.get();
        int position=items.stream().mapToInt(TaskSequenceMapper.Item::position).max().orElse(-1)+1;
        // Unknown titles/slugs are not fabricated. Open the official search by number.
        mapper.insertItem(taskId,position,key,"LeetCode "+key,"https://leetcode.cn/problemset/?search="+key);
        return mapper.items(taskId).stream().filter(i->i.itemKey().equals(key)).findFirst().orElseThrow();
    }
}
