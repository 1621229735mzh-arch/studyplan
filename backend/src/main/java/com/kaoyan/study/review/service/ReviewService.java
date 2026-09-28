package com.kaoyan.study.review.service;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.plan.entity.Task;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.review.dto.ReviewConfirmRequest;
import com.kaoyan.study.review.dto.ReviewConfirmResponse;
import com.kaoyan.study.review.dto.ReviewItemCreateRequest;
import com.kaoyan.study.review.dto.ReviewItemResponse;
import com.kaoyan.study.review.dto.ReviewRecordCreateRequest;
import com.kaoyan.study.review.dto.ReviewRecordResponse;
import com.kaoyan.study.review.dto.ReviewSuggestionResponse;
import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewItem;
import com.kaoyan.study.review.entity.ReviewRecord;
import com.kaoyan.study.review.entity.ReviewStatus;
import com.kaoyan.study.review.mapper.ReviewMapper;
import com.kaoyan.study.settings.entity.StudySettings;
import com.kaoyan.study.settings.service.StudySettingsService;
import com.kaoyan.study.settings.service.SubjectService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 复习用例。
 *
 * <p>遵守 PLAN 中的负担控制规则：
 * <ul>
 *   <li>只有已学过并主动加入复习的内容才会被建议；</li>
 *   <li>建议必须经确认才进入某一天，超额内容留在待安排列表并保留到期信息；</li>
 *   <li>缺少复习额度或预计用时时不宣称符合额度，而是要求补充；</li>
 *   <li>间隔参数来自设置，未设置时不给具体日期建议。</li>
 * </ul>
 */
@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final TaskService taskService;
    private final SubjectService subjectService;
    private final StudyRecordService studyRecordService;
    private final StudySettingsService studySettingsService;

    public ReviewService(ReviewMapper reviewMapper,
                         TaskService taskService,
                         SubjectService subjectService,
                         StudyRecordService studyRecordService,
                         StudySettingsService studySettingsService) {
        this.reviewMapper = reviewMapper;
        this.taskService = taskService;
        this.subjectService = subjectService;
        this.studyRecordService = studyRecordService;
        this.studySettingsService = studySettingsService;
    }

    public List<ReviewItem> list(String status, Long subjectId) {
        return reviewMapper.findItems(status, subjectId);
    }

    public ReviewItem get(Long id) {
        ReviewItem item = reviewMapper.findItemById(id);
        if (item == null) {
            throw new NotFoundException("复习项不存在");
        }
        return item;
    }

    /** 今天已确认安排的复习内容。 */
    public List<ReviewItem> scheduledOn(LocalDate date) {
        return reviewMapper.findScheduledItems(date);
    }

    /**
     * 把内容加入复习。
     *
     * <p>关联任务时必须已经存在学习记录，避免出现“先建复习清单再学习”的反向流程。
     * 同一任务重复加入时返回已有复习项。
     */
    @Transactional
    public ReviewItem addToReview(ReviewItemCreateRequest request) {
        ReviewItem item = new ReviewItem();
        if (request.taskId() != null) {
            if (request.sourceRecordId() != null) {
                throw new BusinessException("REVIEW_TARGET_INVALID", "任务与计划外学习记录请选择一种来源");
            }
            Task task = taskService.requireExistingForUpdate(request.taskId());
            if (studyRecordService.list(null, LocalDate.now(), request.taskId(), null).stream()
                    .noneMatch(this::hasStudyEvidence)) {
                throw new BusinessException("REVIEW_REQUIRES_STUDY", "只能把已经学过的内容加入复习");
            }
            ReviewItem existing = reviewMapper.findItemByTaskId(request.taskId());
            if (existing != null) {
                return existing;
            }
            item.setTaskId(task.getId());
            item.setSubjectId(task.getSubjectId());
            item.setTitle(task.getTitle());
        } else {
            if (request.sourceRecordId() == null) {
                throw new BusinessException("REVIEW_REQUIRES_STUDY", "计划外复习必须选择已经学过的学习记录");
            }
            StudyRecordView source = studyRecordService.get(request.sourceRecordId());
            if (source.taskId() != null || source.subjectId() == null || !hasStudyEvidence(source)) {
                throw new BusinessException("REVIEW_REQUIRES_STUDY", "请选择已学且有科目的计划外学习记录；任务内记录请通过任务加入复习");
            }
            if (request.subjectId() != null && !request.subjectId().equals(source.subjectId())) {
                throw new BusinessException("SUBJECT_MISMATCH", "复习科目必须与来源学习记录一致");
            }
            if (request.title() == null || request.title().isBlank()) {
                throw new BusinessException("REVIEW_TARGET_REQUIRED", "请填写复习内容");
            }
            subjectService.requireExisting(source.subjectId());
            item.setSourceRecordId(source.id());
            item.setSubjectId(source.subjectId());
            item.setTitle(request.title().trim());
        }
        item.setEstimatedMinutes(request.estimatedMinutes());
        item.setStatus(ReviewStatus.PENDING);
        // 刚加入的内容立即可安排，不预设间隔
        item.setNextReviewDate(LocalDate.now());
        reviewMapper.insertItem(item);
        return get(item.getId());
    }

    /**
     * 记录一次复习反馈并推导下次日期。
     *
     * <p>手动指定的日期优先；未指定且对应档位的间隔未配置时，
     * 下次日期保持为空，由前端提示补充间隔参数。
     */
    @Transactional
    public ReviewRecordResponse recordReview(ReviewRecordCreateRequest request) {
        ReviewItem item = get(request.reviewItemId());
        if (request.clientToken() != null && !request.clientToken().isBlank()) {
            ReviewRecord existing = reviewMapper.findRecordByToken(request.clientToken());
            if (existing != null) {
                return ReviewRecordResponse.from(existing, existing.getNextReviewDate() != null);
            }
        }

        StudySettings settings = studySettingsService.get();
        Integer intervalDays = intervalFor(settings, request.result());
        LocalDate nextReviewDate = request.nextReviewDate() != null
                ? request.nextReviewDate()
                : (intervalDays == null ? null : request.reviewDate().plusDays(intervalDays));

        ReviewRecord record = new ReviewRecord();
        record.setReviewItemId(item.getId());
        record.setReviewDate(request.reviewDate());
        record.setResult(request.result());
        record.setDurationMinutes(request.durationMinutes());
        record.setNote(request.note());
        record.setNextReviewDate(nextReviewDate);
        record.setIntervalDays(intervalDays);
        record.setClientToken(request.clientToken());

        try {
            reviewMapper.insertRecord(record);
        } catch (DuplicateKeyException ex) {
            ReviewRecord existing = reviewMapper.findRecordByToken(request.clientToken());
            if (existing != null) {
                return ReviewRecordResponse.from(existing, existing.getNextReviewDate() != null);
            }
            throw ex;
        }

        if (reviewMapper.updateItemAfterReview(item.getId(), request.result(), intervalDays,
                nextReviewDate, item.getVersion()) == 0) {
            throw new ConflictException("REVIEW_ITEM_STALE", "该复习内容已在其他设备上修改，请刷新后重试");
        }
        return ReviewRecordResponse.from(record, nextReviewDate != null);
    }

    public List<ReviewRecord> records(Long reviewItemId) {
        get(reviewItemId);
        return reviewMapper.findRecords(reviewItemId);
    }

    /** 当天到期（含积压）的建议，并说明额度与预计用时的缺口。 */
    public ReviewSuggestionResponse suggestions(LocalDate date) {
        StudySettings settings = studySettingsService.get();
        Integer quota = settings.getDailyReviewMinutes();
        int scheduledMinutes = scheduledMinutes(date);

        List<ReviewItem> due = reviewMapper.findDueItems(date);
        List<ReviewSuggestionResponse.Item> items = due.stream()
                .map(item -> new ReviewSuggestionResponse.Item(
                        item.getId(),
                        item.getTitle(),
                        item.getSubjectId(),
                        item.getNextReviewDate(),
                        ChronoUnit.DAYS.between(item.getNextReviewDate(), date),
                        item.getEstimatedMinutes()))
                .toList();

        List<String> warnings = new ArrayList<>();
        if (quota == null) {
            warnings.add("尚未设置每日复习额度，无法判断建议是否在额度内");
        }
        long missingEstimate = due.stream().filter(item -> item.getEstimatedMinutes() == null).count();
        if (missingEstimate > 0) {
            warnings.add("有 " + missingEstimate + " 项缺少预计用时，确认安排时需要补充");
        }

        return new ReviewSuggestionResponse(date, quota, scheduledMinutes,
                quota == null ? null : Math.max(0, quota - scheduledMinutes), items, warnings);
    }

    /**
     * 确认把建议安排到某一天。
     *
     * <p>按额度逐条判断：放得下的置为已安排，放不下的留在待安排列表并给出原因。
     * 重复提交同一批内容不会产生重复安排（已安排的同日内容视为成功）。
     */
    @Transactional
    public ReviewConfirmResponse confirm(ReviewConfirmRequest request) {
        StudySettings settings = studySettingsService.lockForReviewScheduling();
        Integer quota = settings.getDailyReviewMinutes();
        if (quota == null) {
            throw new BusinessException("REVIEW_QUOTA_REQUIRED", "请先在设置中填写每日复习额度，再确认复习安排");
        }

        // 当前读：即便外层事务已建立快照，也不能使用另一个确认事务提交前的额度。
        int usedMinutes = reviewMapper.findScheduledItemsForUpdate(request.date()).stream()
                .mapToInt(item -> item.getEstimatedMinutes() == null ? 0 : item.getEstimatedMinutes()).sum();
        List<ReviewItemResponse> scheduled = new ArrayList<>();
        List<ReviewConfirmResponse.Rejected> rejected = new ArrayList<>();

        for (ReviewConfirmRequest.Item requestItem : request.items()) {
            ReviewItem item = reviewMapper.findItemByIdForUpdate(requestItem.reviewItemId());
            if (item == null) {
                throw new NotFoundException("复习项不存在");
            }
            if (item.getStatus() == ReviewStatus.SCHEDULED && request.date().equals(item.getScheduledDate())) {
                scheduled.add(ReviewItemResponse.from(item));
                continue;
            }

            Integer estimated = requestItem.estimatedMinutes() != null
                    ? requestItem.estimatedMinutes()
                    : item.getEstimatedMinutes();
            if (estimated == null) {
                rejected.add(new ReviewConfirmResponse.Rejected(item.getId(), item.getTitle(),
                        "缺少预计用时，请补充后再安排"));
                continue;
            }
            if (usedMinutes + estimated > quota) {
                rejected.add(new ReviewConfirmResponse.Rejected(item.getId(), item.getTitle(),
                        "超出当日复习额度，剩余 " + Math.max(0, quota - usedMinutes) + " 分钟"));
                continue;
            }

            if (reviewMapper.updateItemSchedule(item.getId(), ReviewStatus.SCHEDULED,
                    request.date(), estimated, item.getVersion()) == 0) {
                rejected.add(new ReviewConfirmResponse.Rejected(item.getId(), item.getTitle(),
                        "该内容已在其他设备上修改，请刷新后重试"));
                continue;
            }
            usedMinutes += estimated;
            scheduled.add(ReviewItemResponse.from(get(item.getId())));
        }

        return new ReviewConfirmResponse(request.date(), quota, usedMinutes, scheduled, rejected);
    }

    /** 归档复习项：不再提醒，但历史复习记录保留。 */
    @Transactional
    public void archive(Long id, Long version) {
        ReviewItem item = get(id);
        if (!item.getVersion().equals(version)) {
            throw new ConflictException("REVIEW_ITEM_STALE", "该复习内容已在其他设备上修改，请刷新后重试");
        }
        if (reviewMapper.archiveItem(id, version) == 0) {
            throw new ConflictException("REVIEW_ITEM_STALE", "该复习内容已在其他设备上修改，请刷新后重试");
        }
    }

    private int scheduledMinutes(LocalDate date) {
        Integer minutes = reviewMapper.sumScheduledMinutes(date);
        return minutes == null ? 0 : minutes;
    }

    private boolean hasStudyEvidence(StudyRecordView record) {
        return !record.recordDate().isAfter(LocalDate.now())
                && ((record.amount() != null && record.amount().signum() > 0)
                || (record.durationMinutes() != null && record.durationMinutes() > 0));
    }

    private Integer intervalFor(StudySettings settings, MasteryResult result) {
        return switch (result) {
            case FORGOT -> settings.getReviewIntervalForgotDays();
            case VAGUE -> settings.getReviewIntervalVagueDays();
            case MASTERED -> settings.getReviewIntervalMasteredDays();
        };
    }
}
