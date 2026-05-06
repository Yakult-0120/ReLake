-- ========================================
-- ReLake 同步任务表
-- ========================================
CREATE TABLE IF NOT EXISTS ds_task (
    id              BIGINT PRIMARY KEY COMMENT '主键ID（雪花算法）',
    name            VARCHAR(100) NOT NULL COMMENT '任务名称',
    datasource_id   BIGINT NOT NULL COMMENT '源数据源ID',
    target_id       BIGINT NOT NULL COMMENT '目标存储ID',
    engine_type     VARCHAR(20) NOT NULL COMMENT '引擎类型: CANAL/FLINK_CDC/DATAX',
    source_tables   TEXT NOT NULL COMMENT '源表列表（逗号分隔）',
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/VALIDATING/READY/RUNNING/FAILED/STOPPED',
    config_json     TEXT COMMENT '引擎专属配置（JSON）',
    cron_expr       VARCHAR(50) COMMENT 'DataX定时表达式',
    job_handle_json TEXT COMMENT 'JobHandle序列化',
    error_message   TEXT COMMENT '最近一次错误信息',
    description     VARCHAR(500) COMMENT '描述/备注',
    create_time     DATETIME NOT NULL COMMENT '创建时间',
    update_time     DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_name (name),
    INDEX idx_datasource (datasource_id),
    INDEX idx_target (target_id),
    INDEX idx_status (status),
    INDEX idx_engine (engine_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同步任务表';
