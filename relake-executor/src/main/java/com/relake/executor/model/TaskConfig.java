package com.relake.executor.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 引擎入参 — 集成服务组装后传递给引擎的完整任务配置
 */
@Data
public class TaskConfig {

    /** 任务 ID（ds_task 主键） */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 引擎类型 */
    private EngineType engineType;

    // ──────── 数据源信息（已解密）────────

    private Long datasourceId;
    private String datasourceName;
    private String datasourceDbType;
    private String datasourceHost;
    private Integer datasourcePort;
    private String datasourceDbName;
    private String datasourceUsername;
    private String datasourcePassword;

    // ──────── 目标存储信息（已解密）────────

    private Long targetId;
    private String targetName;
    private String targetStorageType;
    private String targetEndpoint;
    private String targetAccessKey;
    private String targetSecretKey;
    private String targetBucket;
    private String targetPaimonWarehouse;

    // ──────── 同步表 ────────

    /** 要同步的源表名列表 */
    private List<String> sourceTables;

    // ──────── 引擎专属配置 ────────

    /** 引擎专属配置（JSON 字符串） */
    private String configJson;

    /** 扩展参数 */
    private Map<String, String> extraParams;
}
