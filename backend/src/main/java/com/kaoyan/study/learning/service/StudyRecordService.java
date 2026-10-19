package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.learning.dto.DayLearningTaskView;
import com.kaoyan.study.learning.dto.DayProgressRequest;
import com.kaoyan.study.learning.dto.CompletedProblemNumbersRequest;
import com.kaoyan.study.learning.dto.DayConfirmationRequest;
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
import static com.kaoyan.study.plan.service.TaskActivityService.counted;
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
    private final SequenceLearningService sequences;
    private final ContinuationLearningService continuedLearning;
    private final com.kaoyan.study.plan.service.TaskContinuationService continuations;
    private final com.kaoyan.study.plan.service.TaskActivityService activities;
    private final StudyRecordMapper studyRecordMapper;
    private final TaskService taskService;
    private final SubjectService subjectService;
    private final StudyUnitService studyUnitService;

    public StudyRecordService(StudyRecordMapper studyRecordMapper,
                              TaskService taskService,
                              SubjectService subjectService,
                              StudyUnitService studyUnitService,
                              DailyPlanService dailyPlanService, SequenceLearningService sequences, com.kaoyan.study.plan.service.TaskActivityService activities,
                              ContinuationLearningService continuedLearning,com.kaoyan.study.plan.service.TaskContinuationService continuations) {
        this.continuedLearning=continuedLearning; this.continuations=continuations;
        this.activities=activities;
        this.sequences=sequences;
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

    /** 目录归类通过学习业务服务检查记录版本；不修改原记录。 */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public StudyRecordView lockForClassification(Long id, Long version) {
        if (studyRecordMapper.lockRecord(id) == null) throw new NotFoundException("学习记录不存在");
        var record = get(id);
        if (version == null || !record.version().equals(version))
            throw new ConflictException("STUDY_RECORD_STALE", "记录已修改，请刷新后重新归类");
        return record;
    }

    /**
     * 提交学习记录。
     *
     * <p>带 {@code clientToken} 的重复提交直接返回已有记录，不会生成第二条：
     * 手机端重试或网络重发都不应该造成重复计数。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudyRecordView create(StudyRecordCreateRequest request) {
        return createEntry(request,false);
    }
    private StudyRecordView createEntry(StudyRecordCreateRequest request,boolean activityCompleted) {
        return createEntry(request,activityCompleted,null);
    }
    private StudyRecordView createEntry(StudyRecordCreateRequest request,boolean activityCompleted,BigDecimal continuationDelta) {
        if (request.clientToken() != null && !request.clientToken().isBlank()) {
            StudyRecord existing = studyRecordMapper.findByClientToken(request.clientToken());
            if (existing != null) {
                verifyCountRetry(existing,request.taskId(),request.recordDate(),request.amount());
                sequences.verifyNumberRetry(existing,request.taskId(),request.recordDate(),request.amount(),request.problemNumber());
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
        record.setActivityCompleted(activityCompleted);
        record.setContinuationDelta(continuationDelta);
        applyTaskAndUnit(record, request.taskId(), request.subjectId(), request.unitId(),
                request.amount() != null);

        // Recheck after taking the task lock: an identical retry may have committed while waiting.
        if (request.clientToken()!=null) {
            var retry=studyRecordMapper.findByClientToken(request.clientToken());
            if (retry!=null) { verifyCountRetry(retry,request.taskId(),request.recordDate(),request.amount()); sequences.verifyNumberRetry(retry,request.taskId(),request.recordDate(),request.amount(),request.problemNumber()); return get(retry.getId()); }
        }
        if (continuations.streamId(request.taskId())!=null && continuedLearning.dayTasks(request.recordDate()).stream()
                .noneMatch(t->t.id().equals(request.taskId())))
            throw new ConflictException("CONTINUATION_NOT_AVAILABLE","该内容尚未进入当天安排，请刷新当前学习内容");
        sequences.bind(record,request.sequenceItemId(),request.problemNumber(),null);
        try {
            studyRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            // 并发重复提交：唯一键兜底，返回已存在的那一条
            StudyRecord existing = studyRecordMapper.findByClientToken(request.clientToken());
            if (existing != null) {
                verifyCountRetry(existing,request.taskId(),request.recordDate(),request.amount());
                sequences.verifyNumberRetry(existing,request.taskId(),request.recordDate(),request.amount(),request.problemNumber());
                return get(existing.getId());
            }
            if (continuations.streamId(record.getTaskId())!=null || counted(activities.get(record.getTaskId())) || sequences.state(record.getTaskId(),record.getRecordDate())!=null)
                throw new ConflictException("TOKEN_REUSED","该提交令牌已使用或已撤销，请刷新后重新提交");
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
        continuations.lockForTasks(existing.getTaskId(),request.taskId());
        if (continuations.streamId(existing.getTaskId())!=null && (!Objects.equals(existing.getTaskId(),request.taskId()) || !existing.getRecordDate().equals(request.recordDate())))
            throw new BusinessException("CONTINUATION_IDENTITY","接续进度记录保留原内容与日期；需要更正请删除后重新记录");
        var originalActivity=existing.getTaskId()==null ? null:activities.get(existing.getTaskId());
        var targetActivity=request.taskId()==null ? null:activities.get(request.taskId());
        if (originalActivity!=null && "CHECK_ONLY".equals(originalActivity.mode())
                || targetActivity!=null && "CHECK_ONLY".equals(targetActivity.mode()))
            throw new BusinessException("CONFIRMATION_EDIT","完成确认请在今日勾选或取消，不能修改为数量或用时记录");
        if ((counted(originalActivity) || counted(targetActivity)) && !Objects.equals(existing.getTaskId(),request.taskId()))
            throw new BusinessException("COUNTED_IDENTITY","实际数量记录保留原任务；需要更正归属请删除后重新记录");
        var original=new StudyRecord();
        original.setTaskId(existing.getTaskId());
        original.setSequenceItemId(existing.getSequenceItemId());
        original.setAmount(existing.getAmount());
        if (!Objects.equals(existing.getTaskId(),request.taskId())) existing.setActivityCompleted(false);
        existing.setRecordDate(request.recordDate());
        existing.setAmount(request.amount());
        existing.setReviewAmount(null);
        existing.setDurationMinutes(request.durationMinutes());
        existing.setContent(request.content());
        existing.setNote(request.note());
        applyTaskAndUnit(existing, request.taskId(), request.subjectId(), request.unitId(),
                request.amount() != null);
        if (continuations.streamId(request.taskId())!=null && continuedLearning.dayTasks(request.recordDate()).stream()
                .noneMatch(t->t.id().equals(request.taskId())))
            throw new ConflictException("CONTINUATION_NOT_AVAILABLE","该内容尚未进入当天安排，不能将其他记录改挂到这里");
        sequences.bind(existing,request.sequenceItemId(),request.problemNumber(),original);
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
        continuations.lockForTasks(existing.getTaskId());
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
        var ordinary = plans.entrySet().stream().filter(entry -> {
            var activity=activities.get(entry.getKey());
            return activity==null || !"CONTINUED_STUDY".equals(activity.mode());
        }).map(entry -> {
            var items = entry.getValue();
            var first = items.getFirst();
            var taskRecords = records.stream().filter(x -> entry.getKey().equals(x.taskId())).toList();
            var activity=activities.get(entry.getKey());
            var planned = activity!=null ? null : items.stream().map(DailyPlanItemView::plannedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var completed = taskRecords.stream().map(StudyRecordView::amount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var fraction = counted(activity) && (completed.signum()>0 || taskRecords.stream().anyMatch(r->r.reviewAmount()!=null)) ? BigDecimal.ONE : activity!=null ? (studyRecordMapper.completedActivities(entry.getKey(),date)>0 ? BigDecimal.ONE:BigDecimal.ZERO) : planned.signum() == 0 ? BigDecimal.ONE
                    : completed.divide(planned, 10, RoundingMode.HALF_UP).min(BigDecimal.ONE);
            Integer estimate = items.stream().anyMatch(x -> x.estimatedMinutes() == null) ? null
                    : items.stream().mapToInt(x -> x.estimatedMinutes()).sum();
            var actualMinutes=taskRecords.stream().map(StudyRecordView::durationMinutes).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            var remaining = fraction.compareTo(BigDecimal.ONE) >= 0 ? BigDecimal.ZERO
                    : activity!=null && estimate!=null ? BigDecimal.valueOf(Math.max(0,estimate-actualMinutes))
                    : estimate == null ? null : BigDecimal.valueOf(estimate).multiply(BigDecimal.ONE.subtract(fraction)).setScale(1, RoundingMode.HALF_UP);
            // Includes plan AND record versions, catching quantity/time edits and delete/recreate races.
            String revision = taskService.get(entry.getKey()).getVersion()+":"+(activity==null ? "":activity.mode())+":"+items.stream().sorted(Comparator.comparing(x -> x.id()))
                    .map(x -> "p" + x.id() + ":" + x.version()).collect(Collectors.joining(","))
                    + ";" + taskRecords.stream().sorted(Comparator.comparing(StudyRecordView::id))
                    .map(x -> "r" + x.id() + ":" + x.version()).collect(Collectors.joining(","));
            var sequence=sequences.state(entry.getKey(),date);
            if (sequence!=null) revision += ";sequence:"+sequence.revision();
            if (activity!=null && ("CHECK_ONLY".equals(activity.mode()) || counted(activity)))
                revision += ";confirmation:"+studyRecordMapper.confirmationHistory(entry.getKey(),date);
            // Fixed-size digest keeps the revision bounded even with many sessions.
            try {
                revision = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(revision.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            return new DayLearningTaskView(entry.getKey(), first.subjectId(),
                    first.taskTitle(), planned, completed, fraction.movePointRight(2), estimate, remaining, revision, sequence,activity,first.unitName());
        }).toList();
        return Stream.concat(ordinary.stream(),continuedLearning.dayTasks(date).stream()).toList();
    }

    /** Task lock serializes all confirmations and cancellations for this shared task. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DayLearningTaskView confirmDay(DayConfirmationRequest request) {
        taskService.requireExistingForUpdate(request.taskId());
        var activity=activities.get(request.taskId());
        if (activity==null || !"CHECK_ONLY".equals(activity.mode()))
            throw new BusinessException("CONFIRMATION_TASK","此入口仅用于完成确认型安排");
        var snapshot=dayTasks(request.date()).stream().filter(t->t.id().equals(request.taskId()))
                .findFirst().orElseThrow(()->new NotFoundException("当天安排不存在"));
        boolean completed=snapshot.completionPercent().intValue()==100;
        // A repeated desired state is a no-op; a stale request must never reverse a later change.
        if (completed==request.completed()) return snapshot;
        if (!snapshot.revision().equals(request.revision()))
            throw new ConflictException("DAY_PROGRESS_STALE","当天安排或完成状态已变化，请刷新后重试");
        var retry=studyRecordMapper.findByClientToken(request.clientToken());
        if (retry!=null) throw new ConflictException("TOKEN_REUSED","该提交已处理，请刷新当前状态");
        if (request.completed()) {
            createEntry(new StudyRecordCreateRequest(request.taskId(),null,null,request.date(),null,null,
                    null,null,request.clientToken()),true);
        } else {
            for (var record:list(request.date(),request.date(),request.taskId(),null))
                if (studyRecordMapper.softDelete(record.id(),record.version())!=1)
                    throw new ConflictException("DAY_PROGRESS_STALE","完成记录已变化，请刷新后重试");
        }
        return dayTasks(request.date()).stream().filter(t->t.id().equals(request.taskId())).findFirst().orElseThrow();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudyRecordView recordDayProgress(DayProgressRequest request) {
        taskService.requireExistingForUpdate(request.taskId());
        var retry = studyRecordMapper.findByClientToken(request.clientToken());
        if (retry != null) {
            if (!request.taskId().equals(retry.getTaskId()) || !request.date().equals(retry.getRecordDate()))
                throw new ConflictException("TOKEN_REUSED", "提交令牌已被其他记录使用");
            verifyCountRetry(retry,request.taskId(),request.date(),request.actualAmount());
            sequences.verifyNumberRetry(retry,request.taskId(),request.date(),request.completionPercent()==100 ? BigDecimal.ONE:null,request.problemNumber());
            return get(retry.getId());
        }
        continuations.lockForTasks(request.taskId());
        var snapshot = dayTasks(request.date()).stream().filter(x -> x.id().equals(request.taskId()))
                .findFirst().orElseThrow(() -> new NotFoundException("当天学习安排不存在"));
        if (!snapshot.revision().equals(request.revision()))
            throw new ConflictException("DAY_PROGRESS_STALE", "当天计划或学习记录已变化，请刷新后重试");
        if (snapshot.continuation()!=null) {
            if (request.actualAmount()!=null)
                throw new BusinessException("CONTINUATION_QUANTITY","学习接续只填写完成百分比，不虚构讲数或题数");
            BigDecimal delta=BigDecimal.valueOf(request.completionPercent()).subtract(snapshot.completionPercent());
            if (delta.signum()<0) throw new BusinessException("PROGRESS_DECREASE","累计完成比例不能降低；请修改或删除原学习记录");
            if (continuedLearning.total(request.taskId()).add(delta).compareTo(BigDecimal.valueOf(100))>0)
                throw new BusinessException("CONTINUATION_FUTURE_PROGRESS","该内容已有后续日期进度，请先修改或删除后续记录再补记");
            if (delta.signum()==0 && (request.durationMinutes()==null || request.durationMinutes()==0))
                throw new BusinessException("EMPTY_PROGRESS","请增加完成比例");
            return createEntry(new StudyRecordCreateRequest(request.taskId(),null,null,request.date(),null,
                    request.durationMinutes(),null,request.note(),request.clientToken()),false,delta.signum()>0 ? delta:null);
        }
        if (snapshot.activity()!=null) {
            if ("CHECK_ONLY".equals(snapshot.activity().mode()))
                throw new BusinessException("CONFIRMATION_ENTRY","此安排只需勾选完成，不填写数量或用时");
            if (request.completionPercent()!=0 && request.completionPercent()!=100)
                throw new BusinessException("ACTIVITY_COMPLETION","时间型安排请选择继续记录或结束本次安排；不填写虚构百分比");
            if (!counted(snapshot.activity()) && snapshot.completionPercent().intValue()==100)
                throw new ConflictException("ACTIVITY_ALREADY_COMPLETED","本次安排已结束，请在学习记录中补记或修改");
            return createEntry(new StudyRecordCreateRequest(request.taskId(),null,null,request.date(),request.actualAmount(),request.durationMinutes(),
                    null,request.note(),request.clientToken()),request.completionPercent()==100);
        }
        if (request.actualAmount()!=null) throw new BusinessException("QUANTITY_PROGRESS","数量型任务请使用原有完成比例或学习记录入口");
        if (snapshot.sequence()!=null && request.completionPercent()!=0 && request.completionPercent()!=100)
            throw new BusinessException("SEQUENCE_WHOLE_ITEM","接续任务只在整题完成后推进；尝试请选择未完成");
        if (snapshot.sequence()!=null && "SELECT_BY_NUMBER".equals(snapshot.sequence().mode()))
            return createEntry(new StudyRecordCreateRequest(request.taskId(),null,null,request.date(),
                    request.completionPercent()==100 ? BigDecimal.ONE:null,request.durationMinutes(),null,null,
                    request.clientToken(),null,request.problemNumber()),false);
        var target = snapshot.plannedAmount().multiply(BigDecimal.valueOf(request.completionPercent()))
                .movePointLeft(2).setScale(6, RoundingMode.HALF_UP);
        var delta = target.subtract(snapshot.completedAmount());
        if (delta.signum() < 0)
            throw new BusinessException("PROGRESS_DECREASE", "累计完成比例不能降低；请在学习记录中修改或删除原记录");
        if (delta.signum() == 0 && snapshot.sequence()==null && (request.durationMinutes()==null || request.durationMinutes() == 0))
            throw new BusinessException("EMPTY_PROGRESS", "请增加完成比例");
        return create(new StudyRecordCreateRequest(request.taskId(), null, null, request.date(),
                delta.signum() == 0 ? null : delta, request.durationMinutes(), null, null, request.clientToken(),
                snapshot.sequence()==null || snapshot.sequence().dayItem()==null ? null : snapshot.sequence().dayItem().id(),request.problemNumber()));
    }

    /** The task lock serializes batches, single records, edits and deletes. All inserts roll back together. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<StudyRecordView> recordCompletedProblems(CompletedProblemNumbersRequest request) {
        var numbers=request.problemNumbers();
        if (numbers==null || numbers.isEmpty() || numbers.size()>200
                || numbers.stream().anyMatch(n->n==null || n<1)
                || numbers.stream().distinct().count()!=numbers.size())
            throw new BusinessException("PROBLEM_NUMBERS_INVALID","请填写不重复的正整数题号，每次最多提交200题");
        if (request.clientToken()==null || request.clientToken().isBlank() || request.clientToken().length()>64)
            throw new BusinessException("TOKEN_REQUIRED","请刷新后重新提交");
        taskService.requireExistingForUpdate(request.taskId());
        var state=sequences.state(request.taskId(),request.date());
        if (state==null || !"SELECT_BY_NUMBER".equals(state.mode()))
            throw new BusinessException("PROBLEM_NUMBER_MODE","此入口仅用于自行填写题号的任务");
        var entries=java.util.stream.IntStream.range(0,numbers.size()).mapToObj(index ->
                new StudyRecordCreateRequest(request.taskId(),null,null,request.date(),BigDecimal.ONE,
                        null,null,null,problemBatchToken(request.clientToken(),index),null,numbers.get(index))).toList();
        var existing=entries.stream().map(e->studyRecordMapper.findByClientTokenIncludingDeleted(e.clientToken())).toList();
        if (existing.stream().anyMatch(Objects::nonNull)) {
            // Atomic batches have contiguous tokens. Checking the next token rejects shortened retries;
            // requiring every requested token rejects extended retries. Payload checks reject reordered/edited retries.
            if (existing.stream().anyMatch(Objects::isNull)
                    || studyRecordMapper.findByClientTokenIncludingDeleted(problemBatchToken(request.clientToken(),numbers.size()))!=null)
                throw new ConflictException("TOKEN_REUSED","该提交令牌已被不同题号列表使用，请刷新后重试");
            for (int i=0;i<entries.size();i++) verifyCreateRetry(existing.get(i),entries.get(i));
            return existing.stream().map(r->get(r.getId())).toList();
        }
        var snapshot=dayTasks(request.date()).stream().filter(t->t.id().equals(request.taskId()))
                .findFirst().orElseThrow(()->new NotFoundException("当天学习安排不存在"));
        if (!snapshot.revision().equals(request.revision()))
            throw new ConflictException("DAY_PROGRESS_STALE","当天计划或学习记录已变化，请刷新后重试");
        return entries.stream().map(e->createEntry(e,false)).toList();
    }

    private String problemBatchToken(String token,int index) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("problem-batch:"+token+":"+index).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private void requireActiveRetry(StudyRecord existing) {
        if (existing.getDeletedAt() != null)
            throw new ConflictException("TOKEN_REUSED", "该提交已撤销，请刷新后重新提交");
    }

    /** Compare effective task ownership: omitted subject/unit inherit the task, decimals ignore scale. */
    private void verifyCreateRetry(StudyRecord existing, StudyRecordCreateRequest request) {
        requireActiveRetry(existing);
        verifyCountRetry(existing, request.taskId(), request.recordDate(), request.amount());
        sequences.verifyNumberRetry(existing, request.taskId(), request.recordDate(), request.amount(), request.problemNumber());
        var savedAmount = existing.getReviewAmount() != null ? existing.getReviewAmount() : existing.getAmount();
        boolean sameAmount = savedAmount == null ? request.amount() == null
                : request.amount() != null && savedAmount.compareTo(request.amount()) == 0;
        if (!Objects.equals(existing.getTaskId(), request.taskId())
                || !Objects.equals(existing.getRecordDate(), request.recordDate())
                || !sameAmount
                || (request.taskId() == null || request.subjectId() != null) && !Objects.equals(existing.getSubjectId(), request.subjectId())
                || (request.taskId() == null || request.unitId() != null) && !Objects.equals(existing.getUnitId(), request.unitId())
                || !Objects.equals(existing.getDurationMinutes(), request.durationMinutes())
                || !Objects.equals(existing.getContent(), request.content())
                || !Objects.equals(existing.getNote(), request.note())
                || request.sequenceItemId() != null && !Objects.equals(existing.getSequenceItemId(), request.sequenceItemId()))
            throw new ConflictException("TOKEN_REUSED", "提交令牌已被不同内容使用，请刷新后重试");
    }

    private void verifyCountRetry(StudyRecord existing,Long taskId,LocalDate date,BigDecimal amount) {
        if (!counted(activities.get(existing.getTaskId())) && !counted(activities.get(taskId))) return;
        var saved=existing.getReviewAmount()!=null ? existing.getReviewAmount():existing.getAmount();
        if (!Objects.equals(taskId,existing.getTaskId()) || !date.equals(existing.getRecordDate())
                || saved==null || amount==null || saved.compareTo(amount)!=0)
            throw new ConflictException("TOKEN_REUSED","提交令牌已被不同内容使用，请刷新后重试");
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
            continuations.lockForTasks(taskId);
            var activity=activities.get(taskId);
            if (counted(activity)) {
                if (record.getId()==null && (record.getClientToken()==null || record.getClientToken().isBlank()))
                    throw new BusinessException("TOKEN_REQUIRED","实际数量记录需要提交令牌，请刷新后重试");
                var amount=record.getAmount();
                if (amount==null || amount.signum()<=0 || amount.stripTrailingZeros().scale()>0 || amount.compareTo(new BigDecimal("99999999"))>0)
                    throw new BusinessException("ACTUAL_COUNT_REQUIRED","请填写本次实际数量（1—99999999 的整数），未学习无需提交");
                if (record.getSequenceItemId()!=null || record.getContinuationDelta()!=null)
                    throw new BusinessException("COUNTED_IDENTITY","实际数量记录不能关联接续题目或百分比");
                record.setActivityCompleted(true);
                if ("COUNT_REVIEW".equals(activity.mode())) {
                    record.setReviewAmount(amount);
                    record.setAmount(null);
                }
            }
            if (activity!=null && "CONTINUED_STUDY".equals(activity.mode()) && hasAmount)
                throw new BusinessException("CONTINUATION_QUANTITY","学习接续百分比独立于实际题量，不填写虚构完成量");
            if (activity!=null && "CHECK_ONLY".equals(activity.mode())
                    && (!record.isActivityCompleted() || hasAmount || record.getDurationMinutes()!=null))
                throw new BusinessException("CONFIRMATION_ENTRY","完成确认只能通过当日勾选保存，不能填写数量或时长");
            if (activity!=null && "RECAP".equals(activity.mode()) && hasAmount)
                throw new BusinessException("RECAP_QUANTITY","复盘与订正不增加首次完成量；新学内容请单独记录");
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
            // 内容记录允许没有数量；保留用户选择的归属，不编造计量单位。
            if (subjectId != null) subjectService.requireExisting(subjectId);
            if (unitId != null) studyUnitService.get(unitId);
            record.setSubjectId(subjectId);
            record.setUnitId(unitId);
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
