package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordUpdateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.entity.StudyRecord;
import com.kaoyan.study.learning.mapper.StudyRecordMapper;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习记录用例。
 *
 * <p>完成量只来自这里，创建计划不会增加完成量。记录变更后统计由查询实时推导，
 * 因此修改、删除会自动反映到进度与图表。
 */
@Service
public class StudyRecordService {

    private final StudyRecordMapper studyRecordMapper;
    private final TaskService taskService;
    private final SubjectService subjectService;
    private final StudyUnitService studyUnitService;

    public StudyRecordService(StudyRecordMapper studyRecordMapper,
                              TaskService taskService,
                              SubjectService subjectService,
                              StudyUnitService studyUnitService) {
        this.studyRecordMapper = studyRecordMapper;
        this.taskService = taskService;
        this.subjectService = subjectService;
        this.studyUnitService = studyUnitService;
    }

    public List<StudyRecordView> list(LocalDate from, LocalDate to, Long taskId, Long subjectId) {
        return studyRecordMapper.findAll(from, to, taskId, subjectId);
    }

    public StudyRecordView get(Long id) {
        StudyRecordView view = studyRecordMapper.findView(id);
        if (view == null) {
            throw new NotFoundException("学习记录不存在");
        }
        return view;
    }

    /**
     * 提交学习记录。
     *
     * <p>带 {@code clientToken} 的重复提交直接返回已有记录，不会生成第二条：
     * 手机端重试或网络重发都不应该造成重复计数。
     */
    @Transactional
    public StudyRecordView create(StudyRecordCreateRequest request) {
        if (request.clientToken() != null && !request.clientToken().isBlank()) {
            StudyRecord existing = studyRecordMapper.findByClientToken(request.clientToken());
            if (existing != null) {
                return get(existing.getId());
            }
        }
        StudyRecord record = new StudyRecord();
        record.setRecordDate(request.recordDate());
        record.setAmount(request.amount());
        record.setDurationMinutes(request.durationMinutes());
        record.setContent(request.content());
        record.setNote(request.note());
        record.setClientToken(request.clientToken());
        applyTaskAndUnit(record, request.taskId(), request.subjectId(), request.unitId(),
                request.amount() != null);

        try {
            studyRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            // 并发重复提交：唯一键兜底，返回已存在的那一条
            StudyRecord existing = studyRecordMapper.findByClientToken(request.clientToken());
            if (existing != null) {
                return get(existing.getId());
            }
            throw ex;
        }
        return get(record.getId());
    }

    @Transactional
    public StudyRecordView update(Long id, StudyRecordUpdateRequest request) {
        StudyRecord existing = studyRecordMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("学习记录不存在");
        }
        if (!existing.getVersion().equals(request.version())) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
        existing.setRecordDate(request.recordDate());
        existing.setAmount(request.amount());
        existing.setDurationMinutes(request.durationMinutes());
        existing.setContent(request.content());
        existing.setNote(request.note());
        applyTaskAndUnit(existing, request.taskId(), request.subjectId(), request.unitId(),
                request.amount() != null);
        if (studyRecordMapper.update(existing) == 0) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
        return get(id);
    }

    /** 删除记录（软删除）。统计查询不再计入，进度随之重算。 */
    @Transactional
    public void delete(Long id, Long version) {
        StudyRecord existing = studyRecordMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("学习记录不存在");
        }
        if (!existing.getVersion().equals(version)) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
        if (studyRecordMapper.softDelete(id, version) == 0) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
    }

    /**
     * 确定记录归属的科目与单位。
     *
     * <p>关联任务时以任务为准，避免出现“任务单位是讲、记录单位是页”的脏数据。
     * 计划外学习只有在填写了完成量时才必须给出科目与单位：没有完成量的记录只计入学习时长，
     * 不计入任何单位的进度，因此允许科目与单位为空。
     */
    private void applyTaskAndUnit(StudyRecord record, Long taskId, Long subjectId, Long unitId, boolean hasAmount) {
        if (taskId != null) {
            Task task = taskService.requireExistingForUpdate(taskId);
            if (unitId != null && !unitId.equals(task.getUnitId())) {
                throw new BusinessException("UNIT_MISMATCH", "记录的计量单位必须与任务的计量单位一致");
            }
            if (subjectId != null && !subjectId.equals(task.getSubjectId())) {
                throw new BusinessException("SUBJECT_MISMATCH", "记录的科目必须与任务的科目一致");
            }
            record.setTaskId(taskId);
            record.setSubjectId(task.getSubjectId());
            record.setUnitId(task.getUnitId());
            return;
        }
        record.setTaskId(null);
        if (!hasAmount) {
            // 只记时长：不要求科目与单位，避免为了记 35 分钟而被迫编造一个单位
            record.setSubjectId(null);
            record.setUnitId(null);
            return;
        }
        if (subjectId == null || unitId == null) {
            throw new BusinessException("RECORD_TARGET_REQUIRED", "计划外学习的完成量需要同时选择科目与计量单位");
        }
        subjectService.requireExisting(subjectId);
        studyUnitService.get(unitId);
        record.setSubjectId(subjectId);
        record.setUnitId(unitId);
    }
}
