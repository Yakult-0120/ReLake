-- ============================================================
-- ReLake - 元数据库表结构
-- 数据库: relake_metadata (Docker Compose 自动创建)
-- ============================================================

-- 数据源配置表
CREATE TABLE IF NOT EXISTS ds_datasource (
    id              BIGINT          NOT NULL PRIMARY KEY COMMENT '主键',
    name            VARCHAR(100)    NOT NULL COMMENT '数据源名称',
    db_type         VARCHAR(20)     NOT NULL COMMENT '数据库类型: MYSQL, POSTGRESQL',
    host            VARCHAR(200)    NOT NULL COMMENT '主机地址',
    port            INT             NOT NULL DEFAULT 3306 COMMENT '端口',
    db_name         VARCHAR(100)    NOT NULL COMMENT '数据库名',
    username        VARCHAR(100)    NOT NULL COMMENT '连接用户名',
    password        VARCHAR(500)    NOT NULL COMMENT '连接密码(AES加密)',
    extra_params    VARCHAR(500)    DEFAULT NULL COMMENT '额外JDBC参数',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '备注描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置';

-- 目标存储配置表
CREATE TABLE IF NOT EXISTS ds_target (
    id              BIGINT          NOT NULL PRIMARY KEY COMMENT '主键',
    name            VARCHAR(100)    NOT NULL COMMENT '目标名称',
    storage_type    VARCHAR(20)     NOT NULL DEFAULT 'MINIO' COMMENT '存储类型: MINIO',
    endpoint        VARCHAR(300)    NOT NULL COMMENT 'MinIO地址, 如 http://minio:9000',
    access_key      VARCHAR(200)    NOT NULL COMMENT 'AccessKey',
    secret_key      VARCHAR(500)    NOT NULL COMMENT 'SecretKey(AES加密)',
    bucket          VARCHAR(100)    NOT NULL COMMENT 'Bucket名称',
    region          VARCHAR(50)     DEFAULT 'us-east-1' COMMENT 'Region',
    paimon_warehouse VARCHAR(300)  DEFAULT NULL COMMENT 'Paimon Warehouse路径, 如 s3://bucket/paimon',
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '备注描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标存储配置';
