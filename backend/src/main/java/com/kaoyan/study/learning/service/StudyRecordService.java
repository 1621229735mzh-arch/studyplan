package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.learning.dto.DayLearningTaskView;
import com.kaoyan.study.learning.dto.DayProgressRequest;
import com.kaoyan.study.learning.dto.StudyRecordCreateRequest;
import com.kaoyan.study.learning.dto.StudyRecordUpdateRequest;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.entity.StudyRecord;
import com.kaoyan.study.learning.mapper.StudyRecordMapper;
import com.kaoyan.study.plan.dto.DailyPlanItemView;
import com.kaoyan.study.plan.entity.PlanSource;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.service.DailyPlanService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.settings.service.StudyUnitService;
import com.kaoyan.study.settings.service.SubjectService;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 学习记录用例。
 *
 * <p>完成量只来自这里，创建计划不会增加完成量。记录变更后统计由查询实时推导，
 * 因此修改、删除会自动反映到进度与图表。
 */
@Service
public class StudyRecordService {

    private final DailyPlanService dailyPlanService;
    private final StudyRecordMapper studyRecordMapper;
    private final TaskService taskService;
    private final SubjectService subjectService;
    private final StudyUnitService studyUnitService;

    public StudyRecordService(StudyRecordMapper studyRecordMapper,
                              TaskService taskService,
                              SubjectService subjectService,
                              StudyUnitService studyUnitService,
                              DailyPlanService dailyPlanService) {
        this.dailyPlanService = dailyPlanService;
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudyRecordView update(Long id, StudyRecordUpdateRequest request) {
        StudyRecord existing = studyRecordMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("学习记录不存在");
        }
        Stream.of(existing.getTaskId(), request.taskId()).filter(Objects::nonNull)
                .distinct().sorted().forEach(taskService::requireExistingForUpdate);
        existing = studyRecordMapper.findById(id);
        if (existing == null || !existing.getVersion().equals(request.version())) {
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id, Long version) {
        StudyRecord existing = studyRecordMapper.findById(id);
        if (existing == null) {
            throw new NotFoundException("学习记录不存在");
        }
        if (existing.getTaskId() != null) taskService.requireExistingForUpdate(existing.getTaskId());
        existing = studyRecordMapper.findById(id);
        if (existing == null || !existing.getVersion().equals(version)) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
        if (studyRecordMapper.softDelete(id, version) == 0) {
            throw new ConflictException("STUDY_RECORD_STALE", "该记录已在其他设备上修改，请刷新后重试");
        }
    }


    public List<DayLearningTaskView> dayTasks(LocalDate date) {
        var plans = dailyPlanService.list(date).stream()
                .filter(p -> p.source() != PlanSource.REVIEW)
                .collect(Collectors.groupingBy(
                    DailyPlanItemView::taskId, LinkedHashMap::new,
                    Collectors.toList()));
        var records = list(date, date, null, null);
        return plans.entrySet().stream().map(entry -> {
            var items = entry.getValue();
            var first = items.getFirst();
            var taskRecords = records.stream().filter(x -> entry.getKey().equals(x.taskId())).toList();
            var planned = items.stream().map(DailyPlanItemView::plannedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var completed = taskRecords.stream().map(StudyRecordView::amount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var fraction = planned.signum() == 0 ? BigDecimal.ONE
                    : completed.divide(planned, 10, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            Integer estimate = items.stream().anyMatch(x -> x.estimatedMinutes() == null) ? null
                    : items.stream().mapToInt(x -> x.estimatedMinutes()).sum();
            var remaining = fraction.compareTo(BigDecimal.ONE) >= 0 ? BigDecimal.ZERO
                    : estimate == null ? null : BigDecimal.valueOf(estimate).multiply(BigDecimal.ONE.subtract(fraction)).setScale(1, RoundingMode.HALF_UP);
            // Includes plan AND record versions, catching quantity/time edits and delete/recreate races.
            String revision = items.stream().sorted(Comparator.comparing(x -> x.id()))
                    .map(x -> "p" + x.id() + ":" + x.version()).collect(Collectors.joining(","))
                    + ";" + taskRecords.stream().sorted(Comparator.comparing(StudyRecordView::id))
                    .map(x -> "r" + x.id() + ":" + x.version()).collect(Collectors.joining(","));
            // Fixed-size digest keeps the revision bounded even with many sessions.
            try {
                revision = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(revision.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            return new DayLearningTaskView(entry.getKey(), first.subjectId(),
                    first.taskTitle(), planned, completed, fraction.movePointRight(2), estimate, remaining, revision);
        }).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudyRecordView recordDayProgress(DayProgressRequest request) {
        taskService.requireExistingForUpdate(request.taskId());
        var retry = studyRecordMapper.findByClientToken(request.clientToken());
        if (retry != null) {
            if (!request.taskId().equals(retry.getTaskId()) || !request.date().equals(retry.getRecordDate()))
                throw new ConflictException("TOKEN_REUSED", "提交令牌已被其他记录使用");
            return get(retry.getId());
        }
        var snapshot = dayTasks(request.date()).stream().filter(x -> x.id().equals(request.taskId()))
                .findFirst().orElseThrow(() -> new NotFoundException("当天学习安排不存在"));
        if (!snapshot.revision().equals(request.revision()))
            throw new ConflictException("DAY_PROGRESS_STALE", "当天计划或学习记录已变化，请刷新后重试");
        var target = snapshot.plannedAmount().multiply(BigDecimal.valueOf(request.completionPercent()))
                .movePointLeft(2).setScale(6, RoundingMode.HALF_UP);
        var delta = target.subtract(snapshot.completedAmount());
        if (delta.signum() < 0)
            throw new BusinessException("PROGRESS_DECREASE", "累计完成比例不能降低；请在学习记录中修改或删除原记录");
        if (delta.signum() == 0 && request.durationMinutes() == 0)
            throw new BusinessException("EMPTY_PROGRESS", "请增加完成比例或填写本次用时");
        return create(new StudyRecordCreateRequest(request.taskId(), null, null, request.date(),
                delta.signum() == 0 ? null : delta, request.durationMinutes(), null, null, request.clientToken()));
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
