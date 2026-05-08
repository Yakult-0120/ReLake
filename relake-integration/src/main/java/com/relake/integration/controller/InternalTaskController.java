package com.relake.integration.controller;

import com.relake.common.dto.DataXConfigDTO;
import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.executor.engine.DataXEngine;
import com.relake.executor.model.EngineType;
import com.relake.executor.model.TaskConfig;
import com.relake.integration.dto.DatasourceDTO;
import com.relake.integration.dto.TargetDTO;
import com.relake.integration.entity.Task;
import com.relake.integration.feign.MetadataClient;
import com.relake.integration.service.impl.TaskServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 内部 API — 供 Job Agent (XXL-JOB Executor) 回调获取任务配置
 */
@Slf4j
@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class InternalTaskController {

    private final TaskServiceImpl taskService;
    private final MetadataClient metadataClient;
    private final DataXEngine dataXEngine;

    /**
     * 获取 DataX 任务配置（含完整命令、参数、DataX JSON）
     */
    @GetMapping("/{taskId}/datax-config")
    public DataXConfigDTO getDataXConfig(@PathVariable Long taskId) {
        log.info("Internal API: 获取 DataX 配置, taskId={}", taskId);

        Task task = taskService.getEntity(taskId);

        // 获取解密后的数据源和目标配置
        R<DatasourceDTO> dsR = metadataClient.getDatasource(task.getDatasourceId());
        R<TargetDTO> targetR = metadataClient.getTarget(task.getTargetId());

        if (!dsR.isSuccess() || dsR.getData() == null) {
            throw new com.relake.common.web.BusinessException(ResultCode.INTERNAL_ERROR, "数据源不可用");
        }
        if (!targetR.isSuccess() || targetR.getData() == null) {
            throw new com.relake.common.web.BusinessException(ResultCode.INTERNAL_ERROR, "目标存储不可用");
        }

        DatasourceDTO ds = dsR.getData();
        TargetDTO target = targetR.getData();

        // 组装 TaskConfig
        TaskConfig config = new TaskConfig();
        config.setTaskId(task.getId());
        config.setTaskName(task.getName());
        config.setEngineType(EngineType.valueOf(task.getEngineType()));
        config.setDatasourceId(ds.getId());
        config.setDatasourceName(ds.getName());
        config.setDatasourceDbType(ds.getDbType());
        config.setDatasourceHost(ds.getHost());
        config.setDatasourcePort(ds.getPort());
        config.setDatasourceDbName(ds.getDbName());
        config.setDatasourceUsername(ds.getUsername());
        config.setDatasourcePassword(ds.getPassword());
        config.setTargetId(target.getId());
        config.setTargetName(target.getName());
        config.setTargetStorageType(target.getStorageType());
        config.setTargetEndpoint(target.getEndpoint());
        config.setTargetAccessKey(target.getAccessKey());
        config.setTargetSecretKey(target.getSecretKey());
        config.setTargetBucket(target.getBucket());
        config.setTargetPaimonWarehouse(target.getPaimonWarehouse());
        config.setSourceTables(Arrays.asList(task.getSourceTables().split(",")));
        config.setConfigJson(task.getConfigJson());

        // 生成 DataX JSON
        String jobJson = dataXEngine.buildDataXJson(config);
        String configFileName = "datax-job-" + taskId + ".json";

        DataXConfigDTO dto = new DataXConfigDTO();
        dto.setCommand("python");
        dto.setArgs("bin/datax.py " + configFileName);
        dto.setWorkingDir("/opt/datax");
        dto.setJobJson(jobJson);

        log.info("DataX 配置已构建: taskId={}, tables={}", taskId, config.getSourceTables());
        return dto;
    }
}
