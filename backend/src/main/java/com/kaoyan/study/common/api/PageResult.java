package com.kaoyan.study.common.api;

import java.util.List;

/**
 * 分页查询结果。
 *
 * @param items 当前页数据
 * @param page  从 0 开始的页码
 * @param size  每页条数
 * @param total 满足条件的总条数
 */
public record PageResult<T>(List<T> items, int page, int size, long total) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, long total) {
        return new PageResult<>(items, page, size, total);
    }
}
