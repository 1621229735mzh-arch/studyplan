package com.kaoyan.study.memo.service;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.memo.dto.MemoConvertRequest;
import com.kaoyan.study.memo.dto.MemoRequest;
import com.kaoyan.study.memo.entity.Memo;
import com.kaoyan.study.memo.mapper.MemoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 备忘录用例。
 *
 * <p>备忘录是随手记：内容与状态只反映本人的记录，到期不自动顺延、也不自动转任务；
 * 转任务必须由本人确认标题与科目/单位后，交给 plan 模块创建。
 */
@Service
public class MemoService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_DONE = "DONE";

    private final MemoMapper memoMapper;
    private final MemoTaskConverter memoTaskConverter;

    public MemoService(MemoMapper memoMapper, MemoTaskConverter memoTaskConverter) {
        this.memoMapper = memoMapper;
        this.memoTaskConverter = memoTaskConverter;
    }

    /** 列表；三个筛选条件都可不传，传空字符串等同于不筛选。 */
    public List<Memo> list(String status, Long subjectId, String keyword) {
        return memoMapper.findAll(normalizeStatus(status), subjectId, normalizeKeyword(keyword));
    }

    public Memo get(Long id) {
        Memo memo = memoMapper.findById(id);
        if (memo == null) {
            throw new NotFoundException("备忘录不存在");
        }
        return memo;
    }

    /** 到期提醒：仍为 OPEN 且到期日不晚于指定日期；date 为空按当天处理。 */
    public List<Memo> listDue(LocalDate date) {
        return memoMapper.findDue(date == null ? LocalDate.now() : date);
    }

    @Transactional
    public Memo create(MemoRequest request) {
        String status = normalizeStatus(request.status());
        Memo memo = new Memo();
        memo.setSubjectId(request.subjectId());
        memo.setContent(request.content().trim());
        memo.setDueDate(request.dueDate());
        memo.setStatus(status == null ? STATUS_OPEN : status);
        // 新建的记录还没有提醒过，忽略标记从“未忽略”开始。
        memo.setReminderDismissed(false);
        memoMapper.insert(memo);
        return memoMapper.findById(memo.getId());
    }

    /**
     * 修改备忘录。
     *
     * <p>版本检查以 UPDATE 的受影响行数为准：读取与写入之间若被其他设备改过就是 0 行，
     * 此时拒绝覆盖，而不是拿读取时的旧值直接写回。
     */
    @Transactional
    public Memo update(Long id, MemoRequest request) {
        Memo existing = get(id);
        String status = normalizeStatus(request.status());
        existing.setSubjectId(request.subjectId());
        existing.setContent(request.content().trim());
        existing.setDueDate(request.dueDate());
        if (status != null) {
            existing.setStatus(status);
        }
        existing.setVersion(request.version());
        int updated = memoMapper.update(existing);
        if (updated == 0) {
            throw new ConflictException("MEMO_STALE", "该备忘录已在其他设备上修改，请刷新后重试");
        }
        return memoMapper.findById(id);
    }

    /**
     * 删除备忘录。
     *
     * <p>不连带删除已转成的任务：任务已经进入计划，删除笔记不应改变计划事实。
     */
    @Transactional
    public void delete(Long id) {
        get(id);
        memoMapper.deleteById(id);
    }

    /** 忽略本次到期提醒；重复忽略无副作用，因此不做版本校验。 */
    @Transactional
    public Memo dismissReminder(Long id) {
        Memo memo = get(id);
        if (!memo.isReminderDismissed()) {
            memo.setReminderDismissed(true);
            memoMapper.dismissReminder(id);
        }
        return memoMapper.findById(id);
    }

    /**
     * 转为计划任务。
     *
     * <p>任务由 plan 模块创建（本模块不写 {@code task} 表）；成功后只回写
     * {@code converted_task_id}，状态保持原样，备忘录原文继续作为任务的来源笔记。
     */
    @Transactional
    public Memo convertToTask(Long id, MemoConvertRequest request) {
        Memo memo = memoMapper.findByIdForUpdate(id);
        if (memo == null) {
            throw new NotFoundException("备忘录不存在");
        }
        if (memo.getConvertedTaskId() != null) {
            return memo;
        }
        Long taskId = memoTaskConverter.convertToTask(request);
        memo.setConvertedTaskId(taskId);
        memoMapper.updateConvertedTask(id, taskId);
        return memoMapper.findById(id);
    }

    /** 校验状态取值；返回 null 表示“未提供”，列表即不过滤、修改即保持原状态。 */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_OPEN.equals(value) && !STATUS_DONE.equals(value)) {
            throw new ConflictException("MEMO_STATUS_INVALID", "备忘录状态只能是 OPEN 或 DONE");
        }
        return value;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
