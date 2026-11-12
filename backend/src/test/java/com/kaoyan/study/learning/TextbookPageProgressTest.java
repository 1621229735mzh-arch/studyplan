package com.kaoyan.study.learning;

import com.kaoyan.study.learning.service.TextbookPageProgress;
import jakarta.validation.Validation;
import com.kaoyan.study.learning.dto.DayProgressRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TextbookPageProgressTest {
    @Test void readsPrintedPagesAndMergesContinuousReadingWithoutDoubleCountingBoundaryPage() {
        var range=TextbookPageProgress.range("高数主教材","教材28—47页：阅读。印刷28—47页（PDF34—53页）。\n教材47—64页：阅读。印刷47—64页（PDF53—70页）。");
        assertThat(range).isEqualTo(new TextbookPageProgress.Range(28,64));
        assertThat(range.percent(28)).isEqualByComparingTo("2.70");
        assertThat(range.percent(64)).isEqualByComparingTo("100");
        assertThat(TextbookPageProgress.range("线性代数主教材","教材第9—14页（PDF第15—20页）。"))
                .isEqualTo(new TextbookPageProgress.Range(9,14));
    }
    @Test void neverTreatsExercisePagesPdfPagesGapsOrAmbiguousRangesAsReadingProgress() {
        for (String description:new String[]{"独立完成习题。印刷25—28页", "阅读印刷25—28页\n习题（独立练习）：印刷28—29页",
                "阅读PDF25—28页", "阅读印刷25—28页\n阅读印刷31—35页", "阅读教材25—28页；印刷26—29页", "印刷28—25页", "印刷0—28页"})
            assertThat(TextbookPageProgress.range("高数教材",description)).as(description).isNull();
        assertThat(TextbookPageProgress.range("1000题高数","印刷3—4页")).isNull();
        assertThat(TextbookPageProgress.range("英语教材","印刷3—4页")).isNull();
    }
    @Test void inclusivePageProgressRetainsDecimalsAndRejectsOutOfRangePages() {
        var range=new TextbookPageProgress.Range(25,27);
        assertThat(range.percent(25)).isEqualByComparingTo("33.33");
        assertThat(range.percent(26)).isEqualByComparingTo("66.66");
        assertThat(range.percent(27)).isEqualByComparingTo("100");
        assertThatThrownBy(()->range.percent(24)).hasMessageContaining("页码范围");
        assertThatThrownBy(()->range.percent(28)).hasMessageContaining("页码范围");
    }
    @Test void httpContractAcceptsExactlyOneProgressInputAndPositivePage() {
        try (var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            assertThat(validator.validate(input(null,25))).isEmpty();
            assertThat(validator.validate(input(50,null))).isEmpty();
            assertThat(validator.validate(input(null,null))).isNotEmpty();
            assertThat(validator.validate(input(50,25))).isNotEmpty();
            assertThat(validator.validate(input(null,0))).isNotEmpty();
        }
    }
    private DayProgressRequest input(Integer percent,Integer page) {
        return new DayProgressRequest(LocalDate.of(2026,11,11),1L,percent==null ? null : java.math.BigDecimal.valueOf(percent),null,"rev","token",null,null,null,page);
    }
}
