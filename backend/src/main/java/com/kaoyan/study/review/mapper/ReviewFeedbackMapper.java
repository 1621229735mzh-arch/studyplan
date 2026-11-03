package com.kaoyan.study.review.mapper;

import com.kaoyan.study.review.dto.ReviewFeedbackCorrection.Result;
import com.kaoyan.study.review.entity.*;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;

@Mapper
public interface ReviewFeedbackMapper {
    @Select("""
        SELECT review_item_id,record_id,result,next_review_date,scheduled_date,interval_days,version,request_hash
        FROM review_feedback_correction WHERE client_token=#{token}
        """)
    Result retry(String token);

    @Insert("""
        INSERT INTO review_feedback_correction(review_item_id,record_id,previous_result,result,previous_date,
            next_review_date,scheduled_date,interval_days,version,client_token,request_hash)
        VALUES(#{out.reviewItemId},#{out.recordId},#{previous},#{out.result},#{previousDate},
            #{out.nextReviewDate},#{out.scheduledDate},#{out.intervalDays},#{out.version},#{token},#{out.requestHash})
        """)
    void audit(@Param("out") Result out, @Param("previous") MasteryResult previous,
               @Param("previousDate") LocalDate previousDate, @Param("token") String token);

    @Update("UPDATE review_record SET result=#{result},next_review_date=#{date},interval_days=#{days} WHERE id=#{id}")
    void correctRecord(@Param("id") Long id,@Param("result") MasteryResult result,
                       @Param("date") LocalDate date,@Param("days") Integer days);

    @Update("""
        UPDATE review_item SET last_result=#{result},next_review_date=#{date},interval_days=#{days},
            scheduled_date=#{scheduled},status=#{status},date_source=#{source},version=version+1
        WHERE id=#{id} AND version=#{version}
        """)
    int correctItem(@Param("id") Long id,@Param("version") Long version,@Param("result") MasteryResult result,
                    @Param("date") LocalDate date,@Param("days") Integer days,@Param("scheduled") LocalDate scheduled,
                    @Param("status") ReviewStatus status,@Param("source") String source);

    @Update("UPDATE review_item SET date_source=#{source} WHERE id=#{id}")
    void dateSource(@Param("id") Long id,@Param("source") String source);
}
