package com.kaoyan.study.learning.service;

import com.kaoyan.study.common.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Reads explicit printed-page ranges only; exercises and gaps cannot be measured by a page cursor. */
public final class TextbookPageProgress {
    private static final Pattern PAGES=Pattern.compile("(?:印刷|教材)\\s*第?\\s*(\\d{1,6})\\s*[—–－~～至到-]\\s*(\\d{1,6})\\s*页");
    private static final String PREFIX="[教材页码] ";
    public record Range(int startPage,int endPage) {
        public BigDecimal percent(int completedPage) {
            if (completedPage<startPage || completedPage>endPage)
                throw new BusinessException("PAGE_OUT_OF_RANGE","请填写本段印刷页码范围内已学完的最后一页");
            return BigDecimal.valueOf((long)completedPage-startPage+1).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf((long)endPage-startPage+1),2,RoundingMode.DOWN);
        }
        public String note(int completedPage,String note) {
            percent(completedPage);
            return PREFIX+"已学完至第"+completedPage+"页（本段印刷"+startPage+"—"+endPage+"页）"
                    +(note==null || note.isBlank() ? "" : "\n"+note);
        }
    }
    private TextbookPageProgress() {}
    public static boolean hasPageNote(String note) { return note!=null && note.startsWith(PREFIX); }
    public static Range range(String book,String description) {
        if (book==null || !book.contains("教材") || book.contains("1000题")
                || !Pattern.compile("数学|高数|线性代数|线代|概率|基础30讲").matcher(book).find()
                || description==null || description.contains("独立完成") || description.contains("独立练习")) return null;
        int start=-1,end=-1;
        // Each merged paragraph must explicitly identify its pages. Prefer printed pages over a repeated heading.
        for (String paragraph:description.split("\\R")) {
            if (paragraph.isBlank()) continue;
            var matcher=PAGES.matcher(paragraph);
            Integer ownStart=null,ownEnd=null;
            while (matcher.find()) {
                int a=Integer.parseInt(matcher.group(1)),b=Integer.parseInt(matcher.group(2));
                if (a<1 || b<a) return null;
                if (ownStart!=null && (a!=ownStart || b!=ownEnd)) return null;
                ownStart=a; ownEnd=b;
            }
            if (ownStart==null) return null;
            if (start<0) { start=ownStart; end=ownEnd; }
            else {
                if (ownStart<start || ownStart>end+1 || ownEnd<end) return null;
                end=ownEnd;
            }
        }
        return start<0 ? null : new Range(start,end);
    }
}
