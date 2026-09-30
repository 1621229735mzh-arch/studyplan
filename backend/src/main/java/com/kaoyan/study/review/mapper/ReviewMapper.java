package com.kaoyan.study.review.mapper;

import com.kaoyan.study.review.dto.DayReviewTaskView;
import com.kaoyan.study.review.entity.MasteryResult;
import com.kaoyan.study.review.entity.ReviewItem;
import com.kaoyan.study.review.entity.ReviewRecord;
import com.kaoyan.study.review.entity.ReviewStatus;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 复习项与复习记录的数据访问。 */
@Mapper
public interface ReviewMapper {

    List<ReviewItem> findItems(@Param("status") String status, @Param("subjectId") Long subjectId);

    ReviewItem findItemById(@Param("id") Long id);

    ReviewItem findItemByIdForUpdate(@Param("id") Long id);

    ReviewItem findItemByTaskId(@Param("taskId") Long taskId);

    List<ReviewItem> findDueItems(@Param("date") LocalDate date);

    List<ReviewItem> findScheduledItems(@Param("date") LocalDate date);

    List<ReviewItem> findScheduledItemsForUpdate(@Param("date") LocalDate date);

    int insertItem(ReviewItem item);

    /** 记录反馈后回到待安排状态，并更新下次日期与间隔。 */
    int updateItemAfterReview(@Param("id") Long id,
                              @Param("result") MasteryResult result,
                              @Param("intervalDays") Integer intervalDays,
                              @Param("nextReviewDate") LocalDate nextReviewDate,
                              @Param("version") Long version);

    /** 确认安排到某一天。 */
    int updateItemSchedule(@Param("id") Long id,
                           @Param("status") ReviewStatus status,
                           @Param("scheduledDate") LocalDate scheduledDate,
                           @Param("estimatedMinutes") Integer estimatedMinutes,
                           @Param("version") Long version);

    int archiveItem(@Param("id") Long id, @Param("version") Long version);

    List<ReviewRecord> findRecords(@Param("reviewItemId") Long reviewItemId);

    ReviewRecord findRecordByToken(@Param("clientToken") String clientToken);

    int insertRecord(ReviewRecord record);
    int touchItem(@Param("id") Long id, @Param("version") Long version);
    List<DayReviewTaskView> findDayTasks(@Param("date") LocalDate date);

    /** 某天已确认安排的复习预计用时合计（分钟），用于额度校验。 */
    Integer sumScheduledMinutes(@Param("date") LocalDate date);
}
