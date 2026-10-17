package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.entity.StudyRecord;
import com.kaoyan.study.learning.mapper.StudyRecordMapper;
import com.kaoyan.study.plan.service.TaskSequenceService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** No cursor is persisted: editing/deleting records immediately restores missing items. */
@Service
public class SequenceLearningService {
    public record Entry(Long id, String key, String title, String url, boolean completed) {}
    public record State(List<Entry> items, Entry current, Entry dayItem, String revision, boolean actionable, String mode) {
        // Today's offline snapshot carries only its display item, never the complete directory.
        public State summary() { return new State(List.of(),current,dayItem,"",actionable,mode); }
    }
    private final TaskSequenceService sequences;
    private final StudyRecordMapper records;
    public SequenceLearningService(TaskSequenceService sequences, StudyRecordMapper records) {
        this.sequences=sequences; this.records=records;
    }
    public State state(Long taskId, LocalDate date) {
        var definition=sequences.definition(taskId);
        if (definition==null) return null;
        var rows=records.findAll(null,null,taskId,null);
        var done=completed(rows,null);
        var entries=sequences.items(taskId).stream().map(i -> new Entry(i.id(),i.itemKey(),i.title(),i.url(),done.contains(i.id()))).toList();
        var current=entries.stream().filter(i -> !i.completed()).findFirst().orElse(null);
        // Historical attempts retain their actual identity; completed days do not show tomorrow's item.
        var own=rows.stream().filter(r -> r.recordDate().equals(date) && r.sequenceItemId()!=null)
                .sorted(Comparator.comparing((StudyRecordView r) -> r.amount()==null).thenComparing(StudyRecordView::id)).findFirst().orElse(null);
        boolean retainOwn=own!=null && (own.amount()!=null || date.isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"))));
        var dayItem=!retainOwn ? current : entries.stream().filter(i -> i.id().equals(own.sequenceItemId())).findFirst().orElse(null);
        var revision=definition.mode()+":"+records.sequenceHistory(taskId)+":"+rows.stream().sorted(Comparator.comparing(StudyRecordView::id))
                .map(r -> r.id()+":"+r.version()).collect(java.util.stream.Collectors.joining(","));
        boolean inRange=!date.isBefore(definition.startDate()) && !date.isAfter(definition.endDate());
        if ("SELECT_BY_NUMBER".equals(definition.mode())) {
            var actual=own==null ? null:entries.stream().filter(i->i.id().equals(own.sequenceItemId())).findFirst().orElse(null);
            var used=rows.stream().map(StudyRecordView::sequenceItemId).collect(java.util.stream.Collectors.toSet());
            return new State(entries.stream().filter(i->used.contains(i.id())).toList(),null,actual,revision,inRange,definition.mode());
        }
        return new State(entries,current,dayItem,revision,inRange && dayItem!=null && (!dayItem.completed() || rows.stream().anyMatch(r -> r.recordDate().equals(date) && r.amount()!=null)),definition.mode());
    }

    /** Caller holds the task row lock. Binding survives content edits and completion rollback. */
    public void bind(StudyRecord record, Long requestedId, Integer problemNumber, StudyRecord original) {
        var definition=sequences.definition(record.getTaskId());
        if (original!=null && original.getSequenceItemId()!=null && !Objects.equals(original.getTaskId(),record.getTaskId()))
            throw new BusinessException("SEQUENCE_RECORD_TASK","接续记录保留原任务与题目；需要更正归属请删除后重新补记");
        if (definition==null) {
            if (requestedId!=null || problemNumber!=null) throw new BusinessException("SEQUENCE_ITEM_INVALID","普通任务不能绑定练习题目");
            record.setSequenceItemId(null); return;
        }
        if ("SELECT_BY_NUMBER".equals(definition.mode())) {
            bindNumber(record,problemNumber,definition);
            return;
        }
        if (problemNumber!=null) throw new BusinessException("PROBLEM_NUMBER_MODE","该任务仍按固定清单接续，请先转换记录方式");
        var itemId=requestedId;
        if (original!=null && original.getSequenceItemId()!=null) {
            if (itemId!=null && !itemId.equals(original.getSequenceItemId()))
                throw new BusinessException("SEQUENCE_ITEM_IMMUTABLE","记录的题目身份不能替换；请删除后重新补记");
            itemId=original.getSequenceItemId();
        }
        final Long selected=itemId;
        var items=sequences.items(record.getTaskId());
        var item=items.stream().filter(i -> i.id().equals(selected)).findFirst()
                .orElseThrow(() -> new ConflictException("SEQUENCE_ITEM_REQUIRED","请选择当前题目后重试"));
        if (record.getRecordDate().isBefore(definition.startDate()) || record.getRecordDate().isAfter(definition.endDate()))
            throw new BusinessException("SEQUENCE_DATE_RANGE","记录日期须在接续计划起止范围内，不会自动延长计划");
        var amount=record.getAmount();
        if (amount!=null && amount.compareTo(BigDecimal.ONE)!=0)
            throw new BusinessException("SEQUENCE_WHOLE_ITEM","完成整题记 1；尚未完成请留空完成量，不记录部分题量");
        var rows=records.findAll(null,null,record.getTaskId(),null);
        var others=rows.stream().filter(r -> !r.id().equals(record.getId())).toList();
        if (amount!=null) {
            var done=completed(others,null);
            if (done.contains(item.id()) || others.stream().anyMatch(r -> r.recordDate().equals(record.getRecordDate()) && r.amount()!=null))
                throw new ConflictException("SEQUENCE_ALREADY_COMPLETED","该题或当天名额已完成，请刷新；每天最多完成 1 题");
            boolean retained=original!=null && original.getAmount()!=null && Objects.equals(original.getSequenceItemId(),item.id());
            if (!retained) {
                var next=items.stream().filter(i -> !done.contains(i.id())).findFirst().orElse(null);
                if (next==null || !next.id().equals(item.id()))
                    throw new ConflictException("SEQUENCE_STALE","当前题已变化，请刷新后继续第一道未完成题");
            }
        } else if (original==null) {
            var snapshot=state(record.getTaskId(),record.getRecordDate());
            if (snapshot.dayItem()==null || !snapshot.dayItem().id().equals(item.id()))
                throw new ConflictException("SEQUENCE_STALE","当前题已变化，请刷新后记录尝试");
        }
        record.setSequenceItemId(item.id());
    }

    private void bindNumber(StudyRecord record,Integer number,com.kaoyan.study.plan.mapper.TaskSequenceMapper.Definition definition) {
        if (record.getRecordDate().isBefore(definition.startDate()) || record.getRecordDate().isAfter(definition.endDate()))
            throw new BusinessException("SEQUENCE_DATE_RANGE","记录日期须在计划起止范围内");
        if (record.getAmount()!=null && record.getAmount().compareTo(BigDecimal.ONE)!=0)
            throw new BusinessException("SEQUENCE_WHOLE_ITEM","完成整题记 1；尝试未完成请留空完成量");
        var others=records.findAll(null,null,record.getTaskId(),null).stream().filter(r->!r.id().equals(record.getId())).toList();
        if (record.getAmount()!=null && others.stream().anyMatch(r->r.recordDate().equals(record.getRecordDate()) && r.amount()!=null))
            throw new ConflictException("SEQUENCE_ALREADY_COMPLETED","当天已完成一题，请在记录页更正题号");
        var item=sequences.resolveNumber(record.getTaskId(),number);
        if (record.getAmount()!=null && others.stream().anyMatch(r->item.id().equals(r.sequenceItemId()) && r.amount()!=null))
            throw new ConflictException("SEQUENCE_ALREADY_COMPLETED","该题已有完成记录，不重复增加首次完成量");
        record.setSequenceItemId(item.id());
    }

    public void verifyNumberRetry(StudyRecord existing, Long taskId, LocalDate date, BigDecimal amount,Integer number) {
        if (!sequences.selectsNumber(existing.getTaskId()) && !sequences.selectsNumber(taskId)) return;
        var saved=sequences.items(existing.getTaskId()).stream().filter(i->i.id().equals(existing.getSequenceItemId())).findFirst().orElse(null);
        if (!Objects.equals(existing.getTaskId(),taskId) || !existing.getRecordDate().equals(date)
                || !Objects.equals(existing.getAmount()==null ? null:existing.getAmount().stripTrailingZeros(),amount==null ? null:amount.stripTrailingZeros())
                || saved==null || number==null || !saved.itemKey().equals(number.toString()))
            throw new ConflictException("TOKEN_REUSED","提交令牌已被不同题号或记录使用，请刷新后重试");
    }
    private Set<Long> completed(List<StudyRecordView> rows, Long excluded) {
        var result=new HashSet<Long>();
        rows.stream().filter(r -> !r.id().equals(excluded) && r.amount()!=null && r.amount().compareTo(BigDecimal.ONE)==0)
                .map(StudyRecordView::sequenceItemId).filter(Objects::nonNull).forEach(result::add);
        return result;
    }
}
