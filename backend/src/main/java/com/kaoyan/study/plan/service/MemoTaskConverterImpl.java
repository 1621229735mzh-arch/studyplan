package com.kaoyan.study.plan.service;

import com.kaoyan.study.memo.dto.MemoConvertRequest;
import com.kaoyan.study.memo.service.MemoTaskConverter;
import com.kaoyan.study.plan.entity.Task;
import org.springframework.stereotype.Service;

/**
 * 备忘录转任务的实现：由 plan 模块提供，内部走正常任务业务服务。
 *
 * <p>这样 memo 模块不需要直接写 {@code task} 表，科目/单位校验与版本规则仍然集中在
 * {@link TaskService}。
 */
@Service
public class MemoTaskConverterImpl implements MemoTaskConverter {

    private final TaskService taskService;

    public MemoTaskConverterImpl(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public Long convertToTask(MemoConvertRequest request) {
        Task task = taskService.create(request.subjectId(), request.unitId(),
                request.taskTitle(), request.plannedAmount());
        return task.getId();
    }
}
