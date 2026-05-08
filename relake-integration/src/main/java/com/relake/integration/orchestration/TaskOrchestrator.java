package com.relake.integration.orchestration;

import com.relake.common.web.BusinessException;
import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.executor.engine.SyncEngine;
import com.relake.executor.engine.SyncEngineFactory;
import com.relake.executor.model.EngineType;
import com.relake.executor.model.JobHandle;
import com.relake.executor.model.JobStatus;
import com.relake.executor.model.Metrics;
import com.relake.executor.model.TaskConfig;
import com.relake.integration.dto.DatasourceDTO;
import com.relake.integration.dto.TargetDTO;
import com.relake.integration.entity.Task;
import com.relake.integration.feign.MetadataClient;
import com.relake.integration.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

/**
 * 任务编排器 — 核心业务逻辑
 * <p>
 * 负责：配置校验（含 Feign 调用 Metadata 服务）、引擎选择与路由、
 * 任务生命周期管理（状态机驱动）、JobHandle 持久化。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskOrchestrator {

    private final SyncEngineFactory engineFactory;
    private final MetadataClient metadataClient;
    private final TaskStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    /**
     * 校验任务配置：验证数据源/目标存在 → 引擎校验 → 状态 READY
     *
     * @return 更新后的任务
     */
    public Task validate(Task task) {
        stateMachine.transition(TaskStatus.valueOf(task.getStatus()), TaskStatus.VALIDATING);
        task.setStatus(TaskStatus.VALIDATING.name());
        log.info("开始校验任务: id={}, name={}", task.getId(), task.getName());

        try {
            // 1. 通过 Feign 验证数据源是否存在
            log.info("校验步骤1: 调用 MetadataClient.getDatasource({})", task.getDatasourceId());
            R<DatasourceDTO> dsR = metadataClient.getDatasource(task.getDatasourceId());
            log.info("校验步骤1 响应: code={}, message={}, dataType={}",
                    dsR.getCode(), dsR.getMessage(),
                    dsR.getData() != null ? dsR.getData().getClass().getSimpleName() : "null");
            if (!dsR.isSuccess() || dsR.getData() == null) {
                log.error("校验步骤1 失败: isSuccess={}, dataIsNull={}, datasourceId={}",
                        dsR.isSuccess(), dsR.getData() == null, task.getDatasourceId());
                throw new BusinessException(ResultCode.TASK_CONFIG_INVALID,
                        "数据源不存在: " + task.getDatasourceId());
            }
            DatasourceDTO ds = dsR.getData();

            // 2. 通过 Feign 验证目标存储是否存在
            log.info("校验步骤2: 调用 MetadataClient.getTarget({})", task.getTargetId());
            R<TargetDTO> targetR = metadataClient.getTarget(task.getTargetId());
            log.info("校验步骤2 响应: code={}, message={}, dataType={}",
                    targetR.getCode(), targetR.getMessage(),
                    targetR.getData() != null ? targetR.getData().getClass().getSimpleName() : "null");
            if (!targetR.isSuccess() || targetR.getData() == null) {
                log.error("校验步骤2 失败: isSuccess={}, dataIsNull={}, targetId={}",
                        targetR.isSuccess(), targetR.getData() == null, task.getTargetId());
                throw new BusinessException(ResultCode.TASK_CONFIG_INVALID,
                        "目标存储不存在: " + task.getTargetId());
            }
            TargetDTO target = targetR.getData();

            // 3. 获取引擎
            EngineType engineType = EngineType.valueOf(task.getEngineType());
            SyncEngine engine = engineFactory.getEngine(engineType);

            // 4. 组装 TaskConfig 并校验
            TaskConfig config = buildTaskConfig(task, ds, target);
            if (!engine.validate(config)) {
                throw new BusinessException(ResultCode.TASK_CONFIG_INVALID, "引擎校验失败");
            }

            // 5. 校验通过 → READY
            stateMachine.transition(TaskStatus.VALIDATING, TaskStatus.READY);
            task.setStatus(TaskStatus.READY.name());
            task.setErrorMessage(null);
            log.info("任务校验通过: id={}, engine={}", task.getId(), engineType);
        } catch (BusinessException e) {
            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage(e.getMessage());
            log.error("任务校验失败: id={}, error={}", task.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage("校验异常: " + e.getMessage());
            log.error("任务校验异常: id={}", task.getId(), e);
            throw new BusinessException(ResultCode.TASK_CONFIG_INVALID, e.getMessage());
        }

        return task;
    }

    /**
     * 启动任务：组装 TaskConfig → 选择引擎 → 提交 → RUNNING
     * 如果任务处于 FAILED 状态，自动先重新校验
     *
     * @return 包含 JobHandle 信息的更新后任务
     */
    public Task start(Task task) {
        TaskStatus currentStatus = TaskStatus.valueOf(task.getStatus());
        if (currentStatus == TaskStatus.FAILED) {
            log.info("任务状态为 FAILED，自动重新校验: id={}", task.getId());
            task = validate(task);
            currentStatus = TaskStatus.valueOf(task.getStatus());
        }
        stateMachine.transition(currentStatus, TaskStatus.RUNNING);

        try {
            // 1. 获取解密完整信息
            R<DatasourceDTO> dsR = metadataClient.getDatasource(task.getDatasourceId());
            R<TargetDTO> targetR = metadataClient.getTarget(task.getTargetId());

            if (!dsR.isSuccess() || !targetR.isSuccess()) {
                throw new BusinessException(ResultCode.TASK_CONFIG_INVALID, "数据源或目标存储不可用");
            }

            // 2. 获取引擎并提交
            EngineType engineType = EngineType.valueOf(task.getEngineType());
            SyncEngine engine = engineFactory.getEngine(engineType);
            TaskConfig config = buildTaskConfig(task, dsR.getData(), targetR.getData());

            JobHandle handle = engine.submit(config);

            // 3. 持久化 JobHandle
            task.setJobHandleJson(serializeJobHandle(handle));
            task.setStatus(TaskStatus.RUNNING.name());
            task.setErrorMessage(null);
            log.info("任务已启动: id={}, engine={}, jobId={}", task.getId(), engineType, handle.getJobId());
        } catch (BusinessException e) {
            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage(e.getMessage());
            log.error("任务启动失败: id={}, error={}", task.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            task.setStatus(TaskStatus.FAILED.name());
            task.setErrorMessage("启动异常: " + e.getMessage());
            log.error("任务启动异常: id={}", task.getId(), e);
            throw new BusinessException(ResultCode.TASK_START_FAILED, e.getMessage());
        }

        return task;
    }

    /**
     * 停止任务
     */
    public Task stop(Task task) {
        stateMachine.transition(TaskStatus.valueOf(task.getStatus()), TaskStatus.STOPPED);

        try {
            EngineType engineType = EngineType.valueOf(task.getEngineType());
            SyncEngine engine = engineFactory.getEngine(engineType);
            JobHandle handle = deserializeJobHandle(task.getJobHandleJson());
            if (handle != null) {
                engine.stop(handle);
            }
            task.setStatus(TaskStatus.STOPPED.name());
            log.info("任务已停止: id={}", task.getId());
        } catch (Exception e) {
            log.error("停止任务异常: id={}, error={}", task.getId(), e.getMessage());
            task.setStatus(TaskStatus.STOPPED.name());
            task.setErrorMessage("停止时异常: " + e.getMessage());
        }

        return task;
    }

    /**
     * 查询任务运行状态（实时从引擎获取）
     */
    public JobStatus getJobStatus(Task task) {
        if (task.getJobHandleJson() == null) {
            log.warn("getJobStatus: task[{}] jobHandleJson 为空", task.getId());
            return JobStatus.UNKNOWN;
        }

        try {
            EngineType engineType = EngineType.valueOf(task.getEngineType());
            SyncEngine engine = engineFactory.getEngine(engineType);
            JobHandle handle = deserializeJobHandle(task.getJobHandleJson());
            if (handle == null) {
                log.warn("getJobStatus: task[{}] deserializeJobHandle 返回 null, json={}", task.getId(), task.getJobHandleJson());
                return JobStatus.UNKNOWN;
            }
            log.info("getJobStatus: task[{}], engine={}, jobId={}, internalId={}", task.getId(), engineType, handle.getJobId(), handle.getInternalId());
            return engine.getStatus(handle);
        } catch (Exception e) {
            log.error("getJobStatus 异常: task[{}], error={}", task.getId(), e.getMessage(), e);
            return JobStatus.UNKNOWN;
        }
    }

    /**
     * 获取任务运行指标
     */
    public Metrics getMetrics(Task task) {
        if (task.getJobHandleJson() == null) return Metrics.empty();

        try {
            EngineType engineType = EngineType.valueOf(task.getEngineType());
            SyncEngine engine = engineFactory.getEngine(engineType);
            JobHandle handle = deserializeJobHandle(task.getJobHandleJson());
            return handle != null ? engine.getMetrics(handle) : Metrics.empty();
        } catch (Exception e) {
            log.debug("查询引擎指标异常: {}", e.getMessage());
            return Metrics.empty();
        }
    }

    // ──────── 私有方法 ────────

    private TaskConfig buildTaskConfig(Task task, DatasourceDTO ds, TargetDTO target) {
        TaskConfig config = new TaskConfig();
        config.setTaskId(task.getId());
        config.setTaskName(task.getName());
        config.setEngineType(EngineType.valueOf(task.getEngineType()));

        // 数据源
        config.setDatasourceId(ds.getId());
        config.setDatasourceName(ds.getName());
        config.setDatasourceDbType(ds.getDbType());
        config.setDatasourceHost(ds.getHost());
        config.setDatasourcePort(ds.getPort());
        config.setDatasourceDbName(ds.getDbName());
        config.setDatasourceUsername(ds.getUsername());
        config.setDatasourcePassword(ds.getPassword());

        // 目标存储
        config.setTargetId(target.getId());
        config.setTargetName(target.getName());
        config.setTargetStorageType(target.getStorageType());
        config.setTargetEndpoint(target.getEndpoint());
        config.setTargetAccessKey(target.getAccessKey());
        config.setTargetSecretKey(target.getSecretKey());
        config.setTargetBucket(target.getBucket());
        config.setTargetPaimonWarehouse(target.getPaimonWarehouse());

        // 源表
        config.setSourceTables(Arrays.asList(task.getSourceTables().split(",")));

        // 引擎配置
        config.setConfigJson(task.getConfigJson());

        return config;
    }

    private String serializeJobHandle(JobHandle handle) {
        try {
            return objectMapper.writeValueAsString(handle);
        } catch (Exception e) {
            log.error("JobHandle 序列化失败: handle={}, error={}", handle, e.getMessage(), e);
            return null;
        }
    }

    private JobHandle deserializeJobHandle(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, JobHandle.class);
        } catch (Exception e) {
            log.error("JobHandle 反序列化失败: json={}, error={}", json, e.getMessage(), e);
            return null;
        }
    }
}
