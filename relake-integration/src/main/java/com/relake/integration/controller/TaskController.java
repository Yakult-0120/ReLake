package com.relake.integration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relake.common.web.R;
import com.relake.integration.dto.TaskCreateRequest;
import com.relake.integration.dto.TaskUpdateRequest;
import com.relake.integration.dto.TaskVO;
import com.relake.integration.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同步任务管理 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** 创建任务 */
    @PostMapping
    public R<TaskVO> create(@RequestBody TaskCreateRequest request) {
        return R.ok(taskService.create(request));
    }

    /** 分页查询 */
    @GetMapping
    public R<Page<TaskVO>> page(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String keyword) {
        return R.ok(taskService.page(page, size, keyword));
    }

    /** 查询全部 */
    @GetMapping("/list")
    public R<List<TaskVO>> listAll() {
        return R.ok(taskService.listAll());
    }

    /** 查询详情 */
    @GetMapping("/{id}")
    public R<TaskVO> getById(@PathVariable Long id) {
        return R.ok(taskService.getById(id));
    }

    /** 更新任务 */
    @PutMapping("/{id}")
    public R<TaskVO> update(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        return R.ok(taskService.update(id, request));
    }

    /** 删除任务 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return R.ok();
    }

    /** 校验任务配置 */
    @PostMapping("/{id}/validate")
    public R<TaskVO> validate(@PathVariable Long id) {
        return R.ok(taskService.validate(id));
    }

    /** 启动任务 */
    @PostMapping("/{id}/start")
    public R<TaskVO> start(@PathVariable Long id) {
        return R.ok(taskService.start(id));
    }

    /** 停止任务 */
    @PostMapping("/{id}/stop")
    public R<TaskVO> stop(@PathVariable Long id) {
        return R.ok(taskService.stop(id));
    }

    /** 查询任务运行状态 */
    @GetMapping("/{id}/status")
    public R<String> getJobStatus(@PathVariable Long id) {
        return R.ok(taskService.getJobStatus(id));
    }

    /** 查询任务运行指标 */
    @GetMapping("/{id}/metrics")
    public R<Object> getMetrics(@PathVariable Long id) {
        return R.ok(taskService.getMetrics(id));
    }
}
