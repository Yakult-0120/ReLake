package com.relake.executor.model;

/**
 * CDC 采集引擎类型
 */
public enum EngineType {

    /** Canal — MySQL binlog 实时采集 */
    CANAL,

    /** Flink CDC — 全量+增量一体化 */
    FLINK_CDC,

    /** DataX — 离线批量同步 */
    DATAX
}
