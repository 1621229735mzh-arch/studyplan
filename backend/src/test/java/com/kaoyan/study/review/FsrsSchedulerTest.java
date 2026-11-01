package com.kaoyan.study.review;

import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.service.FsrsScheduler;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FsrsSchedulerTest {
    @Test void agreesWithPinnedUpstreamSchedulerWithoutFuzz() {
        // Independently executed v5.0.0 JS, SHA256 b97112fac02cf4ef5bb73129a0b63dd7880895af93303e866e0b87f1fb49671a.
        // Day adapter: Again => tomorrow, no intraday learning steps.
        var ratings=new MasteryResult[]{MasteryResult.MASTERED,MasteryResult.MASTERED,MasteryResult.VAGUE,MasteryResult.FORGOT,MasteryResult.MASTERED};
        int[] elapsed={0,3,15,10,1}, days={3,13,22,1,5};
        double[] stability={3.04,12.86,21.7,2.98,4.74}, difficulty={4.54,4.7,5.79,7.75,7.71};
        FsrsScheduler.Memory state=null;
        for(int i=0;i<ratings.length;i++) {
            var result=FsrsScheduler.review(state,elapsed[i],ratings[i]);
            assertThat(result.intervalDays()).isEqualTo(days[i]);
            assertThat(result.memory().stability()).isCloseTo(stability[i],within(0.000001));
            assertThat(result.memory().difficulty()).isCloseTo(difficulty[i],within(0.000001));
            state=result.memory();
        }
    }
    @Test void differentFirstRetrievalResultsHaveDifferentMemoryAndNoInventedHistory() {
        assertThat(FsrsScheduler.review(null,0,MasteryResult.FORGOT).intervalDays()).isEqualTo(1);
        assertThat(FsrsScheduler.review(null,0,MasteryResult.VAGUE).memory().stability()).isEqualTo(1.18);
        assertThat(FsrsScheduler.review(null,0,MasteryResult.MASTERED).intervalDays()).isEqualTo(3);
    }
    @Test void rejectsInvalidMemoryAndDateAndKeepsIntervalsBounded() {
        assertThatThrownBy(()->FsrsScheduler.review(new FsrsScheduler.Memory(Double.NaN,5),1,MasteryResult.MASTERED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->FsrsScheduler.review(null,-1,MasteryResult.MASTERED)).isInstanceOf(IllegalArgumentException.class);
        assertThat(FsrsScheduler.review(new FsrsScheduler.Memory(100000,1),100000,MasteryResult.MASTERED).intervalDays()).isBetween(1,36500);
    }
}
