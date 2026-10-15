package com.kaoyan.study.learning.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
public record DayProgressRequest(
    @NotNull LocalDate date, @NotNull Long taskId,
    @NotNull @Min(0) @Max(100) Integer completionPercent,
    @NotNull @Min(0) Integer durationMinutes,
    @NotBlank @Size(max=4000) String revision,
    @NotBlank @Size(max=64) String clientToken,
    @DecimalMin("0.000001") @Digits(integer=12,fraction=6) BigDecimal actualAmount,
    @Size(max=500) String note) {
    public DayProgressRequest(LocalDate date,Long taskId,Integer completionPercent,Integer durationMinutes,String revision,String clientToken) {
        this(date,taskId,completionPercent,durationMinutes,revision,clientToken,null,null);
    }
}
