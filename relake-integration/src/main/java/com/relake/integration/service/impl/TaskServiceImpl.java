package com.relake.integration.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.integration.dto.TaskCreateRequest;
import com.relake.integration.dto.TaskUpdateRequest;
import com.relake.integration.dto.TaskVO;
import com.relake.integration.entity.Task;
import com.relake.integration.mapper.TaskMapper;
import com.relake.executor.model.JobStatus;
import com.relake.integration.model.TaskStatus;
import com.relake.integration.orchestration.TaskOrchestrator;
import com.relake.integration.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task>
        implements TaskService {

    private final TaskOrchestrator orchestrator;

    @Override
    public TaskVO create(TaskCreateRequest request) {
        if (lambdaQuery().eq(Task::getName, request.getName()).count() > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "任务名称已存在");
        }

        Task task = new Task();
        BeanUtil.copyProperties(request, task);
        task.setStatus(TaskStatus.DRAFT.name());

        save(task);
        log.info("任务创建成功: id={}, name={}", task.getId(), task.getName());
        return TaskVO.from(task);
    }

    @Override
    public TaskVO update(Long id, TaskUpdateRequest request) {
        Task task = super.getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }

        if (request.getName() != null && !request.getName().isBlank()
                && lambdaQuery().eq(Task::getName, request.getName()).ne(Task::getId, id).count() > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "任务名称已存在");
        }

        BeanUtil.copyProperties(request, task);
        updateById(task);
        log.info("任务更新成功: id={}", id);
        return TaskVO.from(task);
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        log.info("任务删除成功: id={}", id);
    }

    @Override
    public TaskVO getById(Long id) {
        Task task = super.getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return TaskVO.from(task);
    }

    @Override
    public Page<TaskVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Task::getName, keyword)
                   .or().like(Task::getDescription, keyword)
                   .or().like(Task::getEngineType, keyword);
        }
        wrapper.orderByDesc(Task::getCreateTime);

        Page<Task> pg = page(new Page<>(page, size), wrapper);
        Page<TaskVO> voPage = new Page<>(pg.getCurrent(), pg.getSize(), pg.getTotal());
        voPage.setRecords(pg.getRecords().stream().map(TaskVO::from).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<TaskVO> listAll() {
        return lambdaQuery().orderByDesc(Task::getCreateTime)
                .list()
                .stream().map(TaskVO::from).collect(Collectors.toList());
    }

    @Override
    public Task getEntity(Long id) {
        Task task = super.getById(id);
        if (task == null) {
            throw new BusinessException(ResultCode.TASK_NOT_FOUND);
        }
        return task;
    }

    @Override
    public TaskVO validate(Long id) {
        Task task = getEntity(id);
        try {
            task = orchestrator.validate(task);
            updateById(task);
            return TaskVO.from(task);
        } catch (BusinessException e) {
            updateById(task); // 校验失败：持久化 FAILED 状态 + errorMessage
            throw e;
        }
    }

    @Override
    public TaskVO start(Long id) {
        Task task = getEntity(id);
        try {
            task = orchestrator.start(task);
            updateById(task);
            return TaskVO.from(task);
        } catch (BusinessException e) {
            updateById(task); // 启动失败：持久化最新状态
            throw e;
        }
    }

    @Override
    public TaskVO stop(Long id) {
        Task task = getEntity(id);
        try {
            task = orchestrator.stop(task);
            updateById(task);
            return TaskVO.from(task);
        } catch (BusinessException e) {
            updateById(task); // 停止失败：持久化最新状态
            throw e;
        }
    }

    @Override
    public String getJobStatus(Long id) {
        Task task = getEntity(id);
        JobStatus jobStatus = orchestrator.getJobStatus(task);
        // 引擎终态 → 同步到任务状态，前端表格可即时反映
        if (jobStatus == JobStatus.FINISHED || jobStatus == JobStatus.FAILED) {
            if (!jobStatus.name().equals(task.getStatus())) {
                String oldStatus = task.getStatus();
                task.setStatus(jobStatus.name());
                updateById(task);
                log.info("任务状态已同步: taskId={}, {}→{}", id, oldStatus, jobStatus.name());
            }
        }
        return jobStatus.name();
    }

    @Override
    public Object getMetrics(Long id) {
        Task task = getEntity(id);
        return orchestrator.getMetrics(task);
    }
}
