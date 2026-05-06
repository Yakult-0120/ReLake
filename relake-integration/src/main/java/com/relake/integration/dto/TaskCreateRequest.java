package com.relake.integration.dto;

import lombok.Data;

/**
 * 创建同步任务请求
 */
@Data
public class TaskCreateRequest {

    /** 任务名称 */
    private String name;

    /** 源数据源 ID */
    private Long datasourceId;

    /** 目标存储 ID */
    private Long targetId;

    /** 引擎类型: CANAL / FLINK_CDC / DATAX */
    private String engineType;

    /** 源表列表（逗号分隔） */
    private String sourceTables;

    /** 引擎专属配置（JSON） */
    private String configJson;

    /** 定时表达式（DataX 定时调度） */
    private String cronExpr;

    /** 描述 */
    private String description;
}
