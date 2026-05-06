package com.relake.integration.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relake.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同步任务实体 — ds_task 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_task")
public class Task extends BaseEntity {

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

    /** 任务状态 */
    private String status;

    /** 引擎专属配置（JSON） */
    private String configJson;

    /** 定时表达式（DataX 定时调度） */
    private String cronExpr;

    /** JobHandle 序列化 */
    private String jobHandleJson;

    /** 错误信息 */
    private String errorMessage;

    /** 描述 */
    private String description;
}
