package com.relake.integration.controller;

import com.relake.common.dto.DataXConfigDTO;
import com.relake.common.web.BusinessException;
import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.executor.engine.DataXEngine;
import com.relake.executor.model.ColumnMeta;
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

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "数据源不可用");
        }
        if (!targetR.isSuccess() || targetR.getData() == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "目标存储不可用");
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

        // 兜底：旧数据 storageType 可能为 null，默认按 MinIO 处理
        if (config.getTargetStorageType() == null || config.getTargetStorageType().isBlank()) {
            config.setTargetStorageType("MINIO");
            log.warn("目标存储类型为空，默认设为 MINIO, taskId={}", taskId);
        }

        // 查询源表列元数据（hdfswriter 需要显式列定义）
        List<String> tables = config.getSourceTables();
        if (tables != null && !tables.isEmpty()) {
            List<ColumnMeta> columns = queryTableColumns(ds, tables.get(0));
            config.setSourceColumns(columns);
            log.info("源表 {} 共 {} 列", tables.get(0), columns.size());
        }

        // 生成 DataX JSON
        String jobJson = dataXEngine.buildDataXJson(config);
        String configFileName = "datax-job-" + taskId + ".json";

        DataXConfigDTO dto = new DataXConfigDTO();
        dto.setCommand("python");
        dto.setArgs("bin/datax.py " + configFileName);
        dto.setWorkingDir("/opt/datax");
        dto.setJobJson(jobJson);

        // MinIO 上传信息（DataX 完成后由 Job Agent 上传 staging 文件）
        String targetType = config.getTargetStorageType();
        dto.setTargetStorageType(targetType);
        if ("MINIO".equalsIgnoreCase(targetType) || "S3".equalsIgnoreCase(targetType)) {
            dto.setMinioEndpoint(config.getTargetEndpoint());
            dto.setMinioAccessKey(config.getTargetAccessKey());
            dto.setMinioSecretKey(config.getTargetSecretKey());
            dto.setMinioBucket(config.getTargetBucket());
            dto.setStagingPath("/opt/datax/output/" + config.getTaskId());
        }

        log.info("DataX 配置已构建: taskId={}, storageType={}, tables={}",
                taskId, targetType, config.getSourceTables());
        return dto;
    }

    /**
     * 查询 MySQL 源表的列信息
     */
    private List<ColumnMeta> queryTableColumns(DatasourceDTO ds, String tableName) {
        List<ColumnMeta> columns = new ArrayList<>();
        String jdbcUrl = "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort()
                + "/" + ds.getDbName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword());
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            stmt.setString(1, ds.getDbName());
            stmt.setString(2, tableName.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String mysqlType = rs.getString("DATA_TYPE");
                    columns.add(new ColumnMeta(name, ColumnMeta.mysqlTypeToDataX(mysqlType)));
                }
            }
        } catch (SQLException e) {
            log.warn("查询源表列信息失败: table={}, error={}", tableName, e.getMessage());
        }
        return columns;
    }
}
