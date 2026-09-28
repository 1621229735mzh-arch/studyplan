package com.kaoyan.study.memo.service;

import com.kaoyan.study.memo.dto.MemoConvertRequest;

/**
 * 备忘录转任务的口子。
 *
 * <p>转任务属于跨模块写操作，本模块不直接写 {@code task} 表，由 plan 模块提供实现
 * （预期实现内部调用任务业务服务，负责校验科目、单位与预算）。
 *
 * <p>由 plan 模块的 {@code MemoTaskConverterImpl} 实现，调用方通过接口注入，
 * 因此本模块不依赖 plan 模块的具体类型，也不直接写 task 表。
 */
public interface MemoTaskConverter {

    /**
     * 依据备忘录内容创建任务，返回新任务 id。
     *
     * <p>由调用方保证在事务内执行：任务创建成功才回写备忘录的转任务结果。
     */
    Long convertToTask(MemoConvertRequest request);
}
