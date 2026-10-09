package com.kaoyan.study.plan.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record ReorderRequest(@NotNull @Size(max=20000) List<@Valid Item> items) {
    public record Item(@NotNull Long id,@NotNull Long version) { }
}
