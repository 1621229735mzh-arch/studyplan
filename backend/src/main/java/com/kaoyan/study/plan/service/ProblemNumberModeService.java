package com.kaoyan.study.plan.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyRecordUpdateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.learning.service.SequenceLearningService;
import com.kaoyan.study.plan.dto.TaskUpdateRequest;
import com.kaoyan.study.plan.mapper.WorkspaceMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

/** Converts an existing sequence in place; corrections use the normal learning service. */
@Service
public class ProblemNumberModeService {
    public record History(StudyRecordView record, Integer problemNumber) {}
    public record Preview(Long taskId,String title,Long version,long workspaceRevision,String revision,List<History> history) {}
    public record Correction(@NotNull Long id,@NotNull Long version,@NotNull @Min(1) Integer problemNumber) {}
    public record Confirm(@NotBlank @Size(max=200) String title,@NotNull Long version,@NotNull Long workspaceRevision,
                          @NotBlank String revision,@NotNull List<@NotNull @Valid Correction> corrections) {}
    private final TaskService tasks;
    private final TaskSequenceService sequences;
    private final SequenceLearningService learning;
    private final StudyRecordService records;
    private final WorkspaceMapper workspace;
    public ProblemNumberModeService(TaskService tasks,TaskSequenceService sequences,SequenceLearningService learning,
                                    StudyRecordService records,WorkspaceMapper workspace) {
        this.tasks=tasks;this.sequences=sequences;this.learning=learning;this.records=records;this.workspace=workspace;
    }
    @Transactional(readOnly=true)
    public Preview preview(Long id) {
        var task=tasks.get(id);
        var definition=sequences.definition(id);
        if (definition==null || sequences.selectsNumber(id))
            throw new ConflictException("PROBLEM_MODE_UNSUPPORTED","仅固定清单任务需要转换；已转换的题号可在记录页修改");
        var items=sequences.items(id);
        var history=records.list(null,null,id,null).stream()
                .sorted(Comparator.comparing(StudyRecordView::recordDate).thenComparing(StudyRecordView::id))
                .map(r->new History(r,items.stream().filter(i->i.id().equals(r.sequenceItemId()))
                        .map(i->parseNumber(i.itemKey())).filter(Objects::nonNull).findFirst().orElse(null))).toList();
        return new Preview(id,task.getTitle(),task.getVersion(),workspace.revision(),learning.state(id,definition.startDate()).revision(),history);
    }
    private static Integer parseNumber(String key) { try { return Integer.valueOf(key); } catch (NumberFormatException ex) { return null; } }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void confirm(Long id,Confirm request) {
        workspace.lock();
        var task=tasks.requireExistingForUpdate(id);
        var preview=preview(id);
        if (!preview.version().equals(request.version()) || preview.workspaceRevision()!=request.workspaceRevision()
                || !preview.revision().equals(request.revision()))
            throw new ConflictException("PROBLEM_MODE_STALE","任务或记录已变化，请重新预览");
        var corrections=new HashMap<Long,Correction>();
        for (var row:request.corrections()) {
            if (row.problemNumber()==null || row.problemNumber()<1 || corrections.put(row.id(),row)!=null)
                throw new ConflictException("PROBLEM_CORRECTIONS_INVALID","请逐条核对历史题号，不能重复记录");
        }
        if (!corrections.keySet().equals(preview.history().stream().map(h->h.record().id()).collect(java.util.stream.Collectors.toSet())))
            throw new ConflictException("PROBLEM_MODE_STALE","历史记录范围已变化，请重新预览");
        sequences.selectByNumber(id);
        for (var h:preview.history()) {
            var r=h.record();var c=corrections.get(r.id());
            if (!r.version().equals(c.version())) throw new ConflictException("PROBLEM_MODE_STALE","历史记录已变化，请重新预览");
            if (!Objects.equals(h.problemNumber(),c.problemNumber()))
                records.update(r.id(),new StudyRecordUpdateRequest(id,r.subjectId(),r.unitId(),r.recordDate(),r.amount(),
                        r.durationMinutes(),r.content(),r.note(),r.version(),r.sequenceItemId(),c.problemNumber()));
        }
        tasks.update(id,new TaskUpdateRequest(task.getSubjectId(),task.getStageGoalId(),request.title(),task.getUnitId(),task.getPlannedAmount(),task.getStatus(),task.getVersion()));
        workspace.touch();
    }
}
