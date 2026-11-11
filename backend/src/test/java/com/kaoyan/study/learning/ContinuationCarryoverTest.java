package com.kaoyan.study.learning;

import com.kaoyan.study.learning.mapper.ContinuationProgressMapper.Row;
import com.kaoyan.study.learning.service.ContinuationLearningService;
import com.kaoyan.study.plan.service.TaskContinuationService.Definition;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ContinuationCarryoverTest {
    private final LocalDate start=LocalDate.of(2026,11,10);
    private List<Definition> book(int offset) {
        return List.of(definition(1,offset),definition(2,offset+2),definition(3,offset+4));
    }
    private Definition definition(long id,int offset) {
        return new Definition(id,1L,"线性代数教材",(int)id,start.plusDays(offset),60,"内容",1L,
                "内容"+id,"讲",0L,0L,true,start);
    }
    private Row row(long id,long task,int offset,int delta,boolean deleted) {
        return new Row(id,task,start.plusDays(offset),BigDecimal.valueOf(delta),0L,deleted);
    }
    @Test void missedContentStaysRequiredAcrossRotationDaysAndDoesNotAccumulateNewChunks() {
        for (int offset:List.of(1,3,20)) {
            var tasks=ContinuationLearningService.project(book(0),List.of(),start.plusDays(offset));
            assertThat(tasks).hasSize(1);
            var task=tasks.getFirst();
            assertThat(task.id()).isEqualTo(1L);
            assertThat(task.completionPercent()).isEqualByComparingTo("0");
            assertThat(task.continuation().carried()).isTrue();
            assertThat(task.continuation().optional()).isFalse();
            assertThat(task.continuation().referenceDay()).isFalse();
        }
    }
    @Test void partialContentStaysRequiredWhileUnstartedFutureContentRemainsOptional() {
        var tasks=ContinuationLearningService.project(book(0),List.of(row(1,1,0,40,false)),start.plusDays(1));
        assertThat(tasks).extracting(t->t.id()).containsExactly(1L,2L);
        assertThat(tasks.getFirst().completionPercent()).isEqualByComparingTo("40");
        assertThat(tasks.getFirst().remainingMinutes()).isEqualByComparingTo("36");
        assertThat(tasks.getFirst().continuation().optional()).isFalse();
        assertThat(tasks.getLast().continuation().optional()).isTrue();
    }
    @Test void futureBookRemainsOptionalUntilDueUnlessAlreadyStarted() {
        var future=book(10);
        var before=ContinuationLearningService.project(future,List.of(),start.plusDays(1));
        assertThat(before.getFirst().continuation().optional()).isTrue();
        var started=ContinuationLearningService.project(future,List.of(row(1,1,0,40,false)),start.plusDays(1));
        assertThat(started.getFirst().continuation().optional()).isFalse();
        assertThat(started.getLast().continuation().optional()).isTrue();
        var deleted=ContinuationLearningService.project(future,List.of(row(1,1,0,40,true)),start.plusDays(1));
        assertThat(deleted).hasSize(1);
        assertThat(deleted.getFirst().continuation().optional()).isTrue();
    }
    @Test void completedContentDoesNotPullUnstartedFutureMaterialIntoOffDayRequirements() {
        var tasks=ContinuationLearningService.project(book(0),List.of(row(1,1,0,100,false)),start.plusDays(1));
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().id()).isEqualTo(2L);
        assertThat(tasks.getFirst().continuation().optional()).isTrue();
    }
    @Test void completingOverdueContentTodayStillOffersNextChunkAsOptional() {
        var tasks=ContinuationLearningService.project(book(0),List.of(row(1,1,5,100,false)),start.plusDays(5));
        assertThat(tasks).extracting(t->t.id()).containsExactly(1L,2L);
        assertThat(tasks.getFirst().completionPercent()).isEqualByComparingTo("100");
        assertThat(tasks.getLast().continuation().optional()).isTrue();
    }
    @Test void deletingCompletionRestoresOriginalContentAndPreservesLaterPartialTaskIdentity() {
        var tasks=ContinuationLearningService.project(book(0),List.of(row(1,1,0,100,true),row(2,2,0,30,false)),start.plusDays(1));
        assertThat(tasks).extracting(t->t.id()).containsExactly(1L,2L,3L);
        assertThat(tasks.subList(0,2)).allMatch(t->!t.continuation().optional());
        assertThat(tasks.getLast().continuation().optional()).isTrue();
        assertThat(tasks.getFirst().completionPercent()).isEqualByComparingTo("0");
        assertThat(tasks.get(1).completionPercent()).isEqualByComparingTo("30");
    }
}
