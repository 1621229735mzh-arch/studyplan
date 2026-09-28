package com.kaoyan.study.memo;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.memo.dto.MemoConvertRequest;
import com.kaoyan.study.memo.dto.MemoRequest;
import com.kaoyan.study.memo.entity.Memo;
import com.kaoyan.study.memo.mapper.MemoMapper;
import com.kaoyan.study.memo.service.MemoService;
import com.kaoyan.study.memo.service.MemoTaskConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 备忘录用例的单元测试：只验证业务规则，不依赖数据库。 */
@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @Mock
    private MemoMapper memoMapper;

    @Mock
    private MemoTaskConverter memoTaskConverter;

    @InjectMocks
    private MemoService memoService;

    @Test
    void listShouldPassNormalizedFiltersToMapper() {
        when(memoMapper.findAll("OPEN", 2L, "单词")).thenReturn(List.of(memo(0L)));

        List<Memo> result = memoService.list(" open ", 2L, "  单词  ");

        assertThat(result).hasSize(1);
        verify(memoMapper).findAll("OPEN", 2L, "单词");
    }

    @Test
    void listShouldTreatBlankKeywordAsNoFilter() {
        when(memoMapper.findAll(null, null, null)).thenReturn(List.of());

        memoService.list(null, null, "   ");

        verify(memoMapper).findAll(null, null, null);
    }

    @Test
    void createShouldDefaultToOpenAndNotDismissed() {
        Memo[] saved = new Memo[1];
        when(memoMapper.insert(any(Memo.class))).thenAnswer(invocation -> {
            Memo inserted = invocation.getArgument(0);
            inserted.setId(10L);
            saved[0] = inserted;
            return 1;
        });
        when(memoMapper.findById(10L)).thenAnswer(invocation -> saved[0]);

        Memo created = memoService.create(
                new MemoRequest(2L, "  背单词  ", LocalDate.now().plusDays(1), null, 0L));

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getContent()).isEqualTo("背单词");
        assertThat(created.getStatus()).isEqualTo("OPEN");
        assertThat(created.isReminderDismissed()).isFalse();
    }

    @Test
    void createShouldRejectUnknownStatus() {
        Throwable thrown = catchThrowable(() -> memoService.create(
                new MemoRequest(null, "内容", null, "MAYBE", 0L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("MEMO_STATUS_INVALID");
        verify(memoMapper, never()).insert(any(Memo.class));
    }

    @Test
    void updateShouldFailWhenVersionAlreadyChangedOnAnotherDevice() {
        when(memoMapper.findById(1L)).thenReturn(memo(3L));
        when(memoMapper.update(any(Memo.class))).thenReturn(0);

        Throwable thrown = catchThrowable(() -> memoService.update(1L,
                new MemoRequest(2L, "改过的内容", null, "OPEN", 2L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("MEMO_STALE");

        // 写入必须带客户端持有的版本，否则乐观锁形同虚设。
        ArgumentCaptor<Memo> captor = ArgumentCaptor.forClass(Memo.class);
        verify(memoMapper).update(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(2L);
        assertThat(captor.getValue().getContent()).isEqualTo("改过的内容");
    }

    @Test
    void updateShouldFailWhenMemoDoesNotExist() {
        when(memoMapper.findById(9L)).thenReturn(null);

        Throwable thrown = catchThrowable(() -> memoService.update(9L,
                new MemoRequest(null, "内容", null, null, 0L)));

        assertThat(thrown).isInstanceOf(NotFoundException.class);
        verify(memoMapper, never()).update(any(Memo.class));
    }

    @Test
    void dismissReminderShouldMarkOnceAndBeIdempotent() {
        Memo dismissed = memo(0L);
        dismissed.setReminderDismissed(true);
        when(memoMapper.findById(5L)).thenReturn(dismissed);

        Memo result = memoService.dismissReminder(5L);

        assertThat(result.isReminderDismissed()).isTrue();
        // 已经忽略过就不必重复写库。
        verify(memoMapper, never()).dismissReminder(anyLong());
    }

    @Test
    void dismissReminderShouldPersistTheFlag() {
        when(memoMapper.findById(5L)).thenReturn(memo(0L));

        Memo result = memoService.dismissReminder(5L);

        assertThat(result.isReminderDismissed()).isTrue();
        verify(memoMapper).dismissReminder(5L);
    }

    @Test
    void listDueShouldDefaultToToday() {
        when(memoMapper.findDue(LocalDate.now())).thenReturn(List.of(memo(0L)));

        List<Memo> due = memoService.listDue(null);

        assertThat(due).hasSize(1);
        verify(memoMapper).findDue(LocalDate.now());
    }

    @Test
    void listDueShouldUseGivenDate() {
        LocalDate date = LocalDate.now().minusDays(3);
        when(memoMapper.findDue(date)).thenReturn(List.of());

        memoService.listDue(date);

        verify(memoMapper).findDue(date);
    }

    @Test
    void convertToTaskShouldDelegateToConverterAndKeepStatus() {
        Memo source = memo(0L);
        when(memoMapper.findByIdForUpdate(1L)).thenReturn(source);
        when(memoMapper.findById(1L)).thenReturn(source);
        MemoConvertRequest request = new MemoConvertRequest("背 100 个单词", 2L, 3L, new BigDecimal("100"));
        when(memoTaskConverter.convertToTask(request)).thenReturn(42L);

        Memo result = memoService.convertToTask(1L, request);

        // 跨模块：任务由 plan 模块创建，本模块只记录来源关系。
        verify(memoTaskConverter).convertToTask(request);
        verify(memoMapper).updateConvertedTask(1L, 42L);
        assertThat(result.getConvertedTaskId()).isEqualTo(42L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getContent()).isEqualTo("背书");
        // 删除备忘录不得删除已转成的任务，转任务也不得删除备忘录。
        verify(memoMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteShouldRemoveOnlyTheMemo() {
        Memo converted = memo(0L);
        converted.setConvertedTaskId(42L);
        when(memoMapper.findById(1L)).thenReturn(converted);

        memoService.delete(1L);

        // 只删备忘录：已转成的任务属于计划事实，由 plan 模块维护，不随笔记删除。
        verify(memoMapper).deleteById(1L);
        verify(memoMapper, never()).update(any(Memo.class));
    }

    @Test
    void convertToTaskShouldReturnExistingConversionOnRetry() {
        Memo source = memo(1L);
        source.setConvertedTaskId(42L);
        when(memoMapper.findByIdForUpdate(1L)).thenReturn(source);

        Memo result = memoService.convertToTask(1L,
                new MemoConvertRequest("重复请求", 2L, 3L, BigDecimal.ONE));

        assertThat(result.getConvertedTaskId()).isEqualTo(42L);
        verify(memoTaskConverter, never()).convertToTask(any());
        verify(memoMapper, never()).updateConvertedTask(anyLong(), anyLong());
    }

    private Memo memo(long version) {
        Memo memo = new Memo();
        memo.setId(1L);
        memo.setSubjectId(2L);
        memo.setContent("背书");
        memo.setStatus("OPEN");
        memo.setVersion(version);
        return memo;
    }
}
