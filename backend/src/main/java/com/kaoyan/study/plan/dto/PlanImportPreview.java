package com.kaoyan.study.plan.dto;
import java.util.List;
public record PlanImportPreview(String previewToken, long revision, PlanImportDocument document,
        int taskCount,int dayCount,int itemCount,List<String> warnings) { }
