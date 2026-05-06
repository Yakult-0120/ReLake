package com.relake.integration.dto;

import com.relake.integration.entity.Task;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步任务响应 VO
 */
@Data
public class TaskVO {

    private Long id;
    private String name;
    private Long datasourceId;
    private Long targetId;
    private String engineType;
    private String sourceTables;
    private String status;
    private String configJson;
    private String cronExpr;
    private String jobHandleJson;
    private String errorMessage;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static TaskVO from(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setName(task.getName());
        vo.setDatasourceId(task.getDatasourceId());
        vo.setTargetId(task.getTargetId());
        vo.setEngineType(task.getEngineType());
        vo.setSourceTables(task.getSourceTables());
        vo.setStatus(task.getStatus());
        vo.setConfigJson(task.getConfigJson());
        vo.setCronExpr(task.getCronExpr());
        vo.setJobHandleJson(task.getJobHandleJson());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setDescription(task.getDescription());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }
}
