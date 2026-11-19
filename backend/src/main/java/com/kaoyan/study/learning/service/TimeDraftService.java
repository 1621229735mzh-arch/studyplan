package com.kaoyan.study.learning.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import com.kaoyan.study.common.exception.BusinessException;
import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.learning.dto.StudyTimeRange;
import com.kaoyan.study.learning.dto.TimeDraft;
import com.kaoyan.study.learning.mapper.TimeDraftMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TimeDraftService {
    private final TimeDraftMapper mapper;

    public TimeDraftService(TimeDraftMapper mapper) {
        this.mapper = mapper;
    }

    private String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public TimeDraft get(String context) {
        return mapper.find(username(), context);
    }

    public TimeDraft save(String context, LocalDateTime start, String expected) {
        requireMinute(start);
        var draft = new TimeDraft(start, UUID.randomUUID().toString());
        if (expected == null) {
            try {
                mapper.insert(username(), context, draft);
            } catch (DuplicateKeyException ex) {
                throw stale();
            }
        } else if (mapper.replace(username(), context, expected, draft) != 1) {
            throw stale();
        }
        return draft;
    }

    public void remove(String context, String expected) {
        if (mapper.remove(username(), context, expected) != 1) throw stale();
    }

    // Called inside the record transaction. A rejected record also restores its draft.
    public void consume(String context, StudyTimeRange range, Integer minutes) {
        if (range == null) return;
        var draft = get(context);
        if (draft == null || !draft.revision().equals(range.revision())) throw stale();
        requireMinute(range.endedAt());
        long elapsed = Duration.between(draft.startedAt(), range.endedAt()).toMinutes();
        if (elapsed < 1 || minutes == null || elapsed != minutes) {
            throw new BusinessException("INVALID_TIME_RANGE", "请核对起止时间和本次用时，结束时间须晚于开始时间");
        }
        remove(context, range.revision());
    }

    private static void requireMinute(LocalDateTime time) {
        if (time == null || time.getYear() < 1000 || time.getYear() > 9999 || time.getSecond() != 0 || time.getNano() != 0) {
            throw new BusinessException("INVALID_TIME", "请填写精确到分钟的北京时间");
        }
    }

    private static ConflictException stale() {
        return new ConflictException("TIME_DRAFT_STALE", "开始时间已变化，请重新打开并核对");
    }
}
