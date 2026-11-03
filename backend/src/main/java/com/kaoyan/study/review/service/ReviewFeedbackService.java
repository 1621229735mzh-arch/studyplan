package com.kaoyan.study.review.service;

import com.kaoyan.study.common.exception.*;
import com.kaoyan.study.review.dto.ReviewFeedbackCorrection.*;
import com.kaoyan.study.review.dto.ReviewRecordResponse;
import com.kaoyan.study.review.entity.*;
import com.kaoyan.study.review.mapper.*;
import com.kaoyan.study.settings.service.StudySettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReviewFeedbackService {
    private final ReviewMapper reviews;
    private final AutoReviewMapper auto;
    private final ReviewFeedbackMapper corrections;
    private final AutoReviewService automatic;
    private final StudySettingsService settings;
    public ReviewFeedbackService(ReviewMapper reviews, AutoReviewMapper auto, ReviewFeedbackMapper corrections,
                                 AutoReviewService automatic, StudySettingsService settings) {
        this.reviews=reviews; this.auto=auto; this.corrections=corrections; this.automatic=automatic; this.settings=settings;
    }
    private ReviewItem require(Long id, boolean lock) {
        var item=lock ? reviews.findItemByIdForUpdate(id) : reviews.findItemById(id);
        if (item==null) throw new NotFoundException("复习项不存在");
        return item;
    }
    private List<ReviewRecord> ordered(Long id) {
        return reviews.findRecords(id).stream().sorted(Comparator.comparing(ReviewRecord::getReviewDate)
                .thenComparing(ReviewRecord::getId)).toList();
    }
    private String blocked(List<ReviewRecord> rows, ReviewRecord latest) {
        if (latest==null) return "还没有完整复习反馈";
        if (!rows.getLast().getId().equals(latest.getId()) || rows.stream().anyMatch(r->r.getId()>latest.getId()))
            return "已有后续复习进度，不能更正之前的反馈";
        return "";
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Context latest(Long id) {
        var item=require(id,false); var rows=ordered(id);
        var latest=rows.stream().filter(r->r.getResult()!=null).reduce((a,b)->b).orElse(null);
        String reason=blocked(rows,latest);
        return new Context(item.getVersion(),latest==null ? null : ReviewRecordResponse.from(latest,latest.getNextReviewDate()!=null),
                reason.isEmpty(),reason,item.getNextReviewDate(),item.getScheduledDate());
    }
    /** Same lock order as learning, scheduling and feedback. No review fact is appended. */
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Result correct(Long id, Request request) {
        automatic.lockSource(id);
        var item=require(id,true);
        String hash=com.kaoyan.study.plan.service.PlanImportService.digest(id+":"+request);
        var retry=corrections.retry(request.clientToken());
        if (retry!=null) {
            if (!hash.equals(retry.requestHash())) throw new ConflictException("TOKEN_REUSED","提交令牌已用于其他更正");
            return retry;
        }
        if (!item.getVersion().equals(request.version())) throw new ConflictException("REVIEW_ITEM_STALE","复习内容已变化，请重新加载后核对");
        var rows=ordered(id);
        var latest=rows.stream().filter(r->r.getResult()!=null).reduce((a,b)->b).orElse(null);
        String reason=blocked(rows,latest);
        if (!reason.isEmpty()) throw new BusinessException("REVIEW_CORRECTION_BLOCKED",reason);
        if (!latest.getId().equals(request.recordId())) throw new ConflictException("REVIEW_RECORD_STALE","只能更正最近一次完整反馈");
        if (latest.getResult()==request.result()) throw new BusinessException("REVIEW_UNCHANGED","请选择更正后的掌握情况");
        Integer days;
        LocalDate next;
        var state=auto.state(id);
        String source;
        boolean schedule=false;
        if (state!=null) {
            if (!FsrsScheduler.VERSION.equals(state.algorithm())) throw new BusinessException("REVIEW_ALGORITHM_UNKNOWN","历史算法版本不支持重算");
            FsrsScheduler.Memory memory=null;
            LocalDate previous=null;
            int repetitions=0;
            FsrsScheduler.Result calculated=null;
            for (var row:rows) {
                if (row.getResult()==null) continue;
                if (previous!=null && !row.getReviewDate().isAfter(previous))
                    throw new BusinessException("REVIEW_HISTORY_INVALID","历史反馈日期不严格递增，无法安全重算");
                calculated=FsrsScheduler.review(memory,previous==null ? 0 : Math.toIntExact(ChronoUnit.DAYS.between(previous,row.getReviewDate())),
                        row.getId().equals(latest.getId()) ? request.result() : row.getResult());
                memory=calculated.memory(); previous=row.getReviewDate(); repetitions++;
            }
            days=calculated.intervalDays(); next=latest.getReviewDate().plusDays(days); source="FSRS";
            auto.replaceMemory(id,memory.stability(),memory.difficulty(),previous,repetitions);
            // A correction may repair memory while a source or policy is paused; never resume it implicitly.
            if (state.sourceValid() && !state.paused() && Boolean.TRUE.equals(auto.enabled(state.streamId()))) {
                try { automatic.requireSource(item); schedule=true; }
                catch (BusinessException ignored) { schedule=false; }
            }
        } else {
            var config=settings.get();
            days=switch(request.result()) {
                case FORGOT -> config.getReviewIntervalForgotDays();
                case VAGUE -> config.getReviewIntervalVagueDays();
                case MASTERED -> config.getReviewIntervalMasteredDays();
            };
            next=days==null ? null : latest.getReviewDate().plusDays(days); source="SETTINGS";
        }
        // A source may have been deleted and completed again after this historical feedback.
        LocalDate due=next;
        if (state!=null && state.sourceValid() && due.isBefore(state.completedOn().plusDays(1))) {
            due=state.completedOn().plusDays(1); source="SOURCE";
        }
        var status=item.getStatus()==ReviewStatus.ARCHIVED ? ReviewStatus.ARCHIVED : schedule ? ReviewStatus.SCHEDULED : ReviewStatus.PENDING;
        LocalDate scheduled=status==ReviewStatus.SCHEDULED ? due : null;
        corrections.correctRecord(latest.getId(),request.result(),next,days);
        if (corrections.correctItem(id,item.getVersion(),request.result(),due,days,scheduled,status,source)!=1)
            throw new ConflictException("REVIEW_ITEM_STALE","复习内容已变化，请重新加载");
        var result=new Result(id,latest.getId(),request.result(),due,scheduled,days,item.getVersion()+1,hash);
        try { corrections.audit(result,latest.getResult(),item.getNextReviewDate(),request.clientToken()); }
        catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ConflictException("TOKEN_REUSED","提交令牌已用于其他更正，请重新加载");
        }
        return result;
    }
}
