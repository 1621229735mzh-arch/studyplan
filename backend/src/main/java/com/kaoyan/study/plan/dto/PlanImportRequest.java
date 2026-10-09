package com.kaoyan.study.plan.dto;
import jakarta.validation.constraints.*;
public record PlanImportRequest(@NotBlank @Size(max=2000000) String document,
        @Size(max=64) String previewToken) { }
