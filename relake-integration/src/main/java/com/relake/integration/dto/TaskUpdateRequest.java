package com.relake.integration.dto;

import lombok.Data;

/**
 * 更新同步任务请求（仅需传变更字段）
 */
@Data
public class TaskUpdateRequest {

    private String name;
    private Long datasourceId;
    private Long targetId;
    private String engineType;
    private String sourceTables;
    private String configJson;
    private String cronExpr;
    private String description;
}
