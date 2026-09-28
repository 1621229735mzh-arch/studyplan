package com.kaoyan.study.target;

import com.kaoyan.study.common.exception.ConflictException;
import com.kaoyan.study.common.exception.NotFoundException;
import com.kaoyan.study.target.dto.TargetRequest;
import com.kaoyan.study.target.dto.TargetResponse;
import com.kaoyan.study.target.dto.TargetStatusRequest;
import com.kaoyan.study.target.entity.TargetLink;
import com.kaoyan.study.target.entity.TargetSchool;
import com.kaoyan.study.target.mapper.TargetMapper;
import com.kaoyan.study.target.service.TargetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 候选目标用例的单元测试：只验证业务规则，不依赖数据库。 */
@ExtendWith(MockitoExtension.class)
class TargetServiceTest {

    @Mock
    private TargetMapper targetMapper;

    @InjectMocks
    private TargetService targetService;

    @Test
    void listShouldAttachEachTargetsLinks() {
        when(targetMapper.findAll(null)).thenReturn(List.of(target(1L, 0L), target(2L, 0L)));
        when(targetMapper.findLinksByTargetId(1L)).thenReturn(List.of(link(11L, 1L, 0)));
        when(targetMapper.findLinksByTargetId(2L)).thenReturn(List.of());

        List<TargetResponse> result = targetService.list(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).links()).hasSize(1);
        assertThat(result.get(0).links().get(0).url()).isEqualTo("https://a.example.com");
        assertThat(result.get(1).links()).isEmpty();
    }

    @Test
    void listShouldRejectUnknownStatusFilter() {
        Throwable thrown = catchThrowable(() -> targetService.list("MAYBE"));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("TARGET_STATUS_INVALID");
        verify(targetMapper, never()).findAll(any());
    }

    @Test
    void getShouldFailWhenTargetDoesNotExist() {
        when(targetMapper.findById(9L)).thenReturn(null);

        Throwable thrown = catchThrowable(() -> targetService.get(9L));

        assertThat(thrown).isInstanceOf(NotFoundException.class);
        verify(targetMapper, never()).findLinksByTargetId(anyLong());
    }

    @Test
    void createShouldDefaultToCandidateAndKeepSubmittedLinkOrder() {
        TargetSchool[] saved = new TargetSchool[1];
        when(targetMapper.insert(any(TargetSchool.class))).thenAnswer(invocation -> {
            TargetSchool inserted = invocation.getArgument(0);
            inserted.setId(7L);
            saved[0] = inserted;
            return 1;
        });
        when(targetMapper.findById(7L)).thenAnswer(invocation -> saved[0]);
        when(targetMapper.findLinksByTargetId(7L)).thenReturn(List.of());

        TargetResponse created = targetService.create(new TargetRequest("北京大学", "软件工程", null, null, null,
                List.of(new TargetRequest.LinkRequest("https://pku.example.com", "官网")), 0L));

        assertThat(created.status()).isEqualTo("CANDIDATE");

        ArgumentCaptor<TargetLink> linkCaptor = ArgumentCaptor.forClass(TargetLink.class);
        verify(targetMapper).insertLink(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getTargetId()).isEqualTo(7L);
        assertThat(linkCaptor.getValue().getUrl()).isEqualTo("https://pku.example.com");
        assertThat(linkCaptor.getValue().getSortOrder()).isZero();
        // 新建时没有旧链接可删。
        verify(targetMapper, never()).deleteLinksByTargetId(anyLong());
    }

    @Test
    void createShouldRejectUnknownStatus() {
        Throwable thrown = catchThrowable(() -> targetService.create(new TargetRequest("北京大学", null, null,
                "MAYBE", null, List.of(), 0L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("TARGET_STATUS_INVALID");
        verify(targetMapper, never()).insert(any(TargetSchool.class));
    }

    @Test
    void updateShouldReplaceLinksWholesaleAfterVersionCheck() {
        TargetSchool stored = target(1L, 3L);
        when(targetMapper.findById(1L)).thenReturn(stored);
        when(targetMapper.update(any(TargetSchool.class))).thenReturn(1);
        when(targetMapper.findLinksByTargetId(1L)).thenReturn(List.of());

        TargetResponse response = targetService.update(1L, new TargetRequest("清华大学", "计算机科学与技术", "看初试科目",
                null, 5, List.of(
                new TargetRequest.LinkRequest("https://a.example.com", "招生简章"),
                new TargetRequest.LinkRequest("https://b.example.com", null)), 3L));

        ArgumentCaptor<TargetSchool> targetCaptor = ArgumentCaptor.forClass(TargetSchool.class);
        InOrder inOrder = inOrder(targetMapper);
        inOrder.verify(targetMapper).update(targetCaptor.capture());
        // 版本通过后才替换链接，避免过期请求清空旧链接。
        inOrder.verify(targetMapper).deleteLinksByTargetId(1L);
        ArgumentCaptor<TargetLink> linkCaptor = ArgumentCaptor.forClass(TargetLink.class);
        inOrder.verify(targetMapper, times(2)).insertLink(linkCaptor.capture());

        assertThat(targetCaptor.getValue().getVersion()).isEqualTo(3L);
        assertThat(targetCaptor.getValue().getSchoolName()).isEqualTo("清华大学");
        assertThat(targetCaptor.getValue().getSortOrder()).isEqualTo(5);
        assertThat(linkCaptor.getAllValues()).extracting(TargetLink::getUrl)
                .containsExactly("https://a.example.com", "https://b.example.com");
        assertThat(linkCaptor.getAllValues()).extracting(TargetLink::getLabel)
                .containsExactly("招生简章", null);
        assertThat(linkCaptor.getAllValues()).extracting(TargetLink::getSortOrder).containsExactly(0, 1);
        assertThat(response.status()).isEqualTo("CANDIDATE");
    }

    @Test
    void updateShouldFailWhenVersionAlreadyChangedOnAnotherDevice() {
        when(targetMapper.findById(1L)).thenReturn(target(1L, 5L));
        when(targetMapper.update(any(TargetSchool.class))).thenReturn(0);

        Throwable thrown = catchThrowable(() -> targetService.update(1L, new TargetRequest("清华大学", null, null,
                null, null, List.of(new TargetRequest.LinkRequest("https://a.example.com", null)), 4L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("TARGET_STALE");

        // 过期请求不得清空已有链接。
        verify(targetMapper, never()).deleteLinksByTargetId(anyLong());
        verify(targetMapper, never()).insertLink(any(TargetLink.class));
    }

    @Test
    void updateShouldFailWhenTargetDoesNotExist() {
        when(targetMapper.findById(9L)).thenReturn(null);

        Throwable thrown = catchThrowable(() -> targetService.update(9L, new TargetRequest("清华大学", null, null,
                null, null, List.of(), 0L)));

        assertThat(thrown).isInstanceOf(NotFoundException.class);
        verify(targetMapper, never()).update(any(TargetSchool.class));
    }

    @Test
    void updateStatusShouldChangeOnlyStatus() {
        TargetSchool stored = target(1L, 4L);
        when(targetMapper.findById(1L)).thenReturn(stored);
        when(targetMapper.findLinksByTargetId(1L)).thenReturn(List.of());
        when(targetMapper.updateStatus(1L, "CHOSEN", 4L)).thenAnswer(invocation -> {
            stored.setStatus("CHOSEN");
            return 1;
        });

        TargetResponse response = targetService.updateStatus(1L, new TargetStatusRequest("CHOSEN", 4L));

        assertThat(response.status()).isEqualTo("CHOSEN");
        verify(targetMapper).updateStatus(1L, "CHOSEN", 4L);
        verify(targetMapper, never()).update(any(TargetSchool.class));
        verify(targetMapper, never()).deleteLinksByTargetId(anyLong());
    }

    @Test
    void updateStatusShouldFailWhenVersionStale() {
        when(targetMapper.findById(1L)).thenReturn(target(1L, 4L));
        when(targetMapper.updateStatus(1L, "DROPPED", 2L)).thenReturn(0);

        Throwable thrown = catchThrowable(() ->
                targetService.updateStatus(1L, new TargetStatusRequest("DROPPED", 2L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("TARGET_STALE");
    }

    @Test
    void updateStatusShouldRejectUnknownStatus() {
        when(targetMapper.findById(1L)).thenReturn(target(1L, 4L));

        Throwable thrown = catchThrowable(() ->
                targetService.updateStatus(1L, new TargetStatusRequest("MAYBE", 4L)));

        assertThat(thrown).isInstanceOf(ConflictException.class);
        assertThat(((ConflictException) thrown).getCode()).isEqualTo("TARGET_STATUS_INVALID");
        verify(targetMapper, never()).updateStatus(anyLong(), any(), anyLong());
    }

    @Test
    void deleteShouldDeleteOnlyTheTarget() {
        when(targetMapper.findById(1L)).thenReturn(target(1L, 0L));

        targetService.delete(1L);

        verify(targetMapper).deleteById(1L);
        // 链接交给外键 ON DELETE CASCADE，不重复发删除语句。
        verify(targetMapper, never()).deleteLinksByTargetId(anyLong());
    }

    @Test
    void deleteShouldFailWhenTargetDoesNotExist() {
        when(targetMapper.findById(9L)).thenReturn(null);

        Throwable thrown = catchThrowable(() -> targetService.delete(9L));

        assertThat(thrown).isInstanceOf(NotFoundException.class);
        verify(targetMapper, never()).deleteById(anyLong());
    }

    private TargetSchool target(long id, long version) {
        TargetSchool target = new TargetSchool();
        target.setId(id);
        target.setSchoolName("目标院校");
        target.setStatus("CANDIDATE");
        target.setVersion(version);
        return target;
    }

    private TargetLink link(long id, long targetId, int sortOrder) {
        TargetLink link = new TargetLink();
        link.setId(id);
        link.setTargetId(targetId);
        link.setUrl("https://a.example.com");
        link.setLabel("招生简章");
        link.setSortOrder(sortOrder);
        return link;
    }
}
