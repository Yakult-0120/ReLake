package com.relake.integration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.relake.integration.dto.TaskCreateRequest;
import com.relake.integration.dto.TaskUpdateRequest;
import com.relake.integration.dto.TaskVO;
import com.relake.integration.entity.Task;

import java.util.List;

public interface TaskService extends IService<Task> {

    TaskVO create(TaskCreateRequest request);

    TaskVO update(Long id, TaskUpdateRequest request);

    void delete(Long id);

    TaskVO getById(Long id);

    Page<TaskVO> page(int page, int size, String keyword);

    List<TaskVO> listAll();

    /** 获取完整实体 */
    Task getEntity(Long id);

    /** 校验任务配置 */
    TaskVO validate(Long id);

    /** 启动任务 */
    TaskVO start(Long id);

    /** 停止任务 */
    TaskVO stop(Long id);

    /** 获取任务运行状态 */
    String getJobStatus(Long id);

    /** 获取任务运行指标 */
    Object getMetrics(Long id);

    /** 获取任务运行日志 */
    String getJobLog(Long id);
}
