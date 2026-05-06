package com.relake.executor.engine;

import com.relake.executor.model.*;

/**
 * CDC 同步引擎抽象接口 — 策略模式核心
 * <p>
 * 每种采集引擎（Canal、Flink CDC、DataX）实现此接口，
 * 通过 {@link SyncEngineFactory} 根据任务类型自动路由。
 */
public interface SyncEngine {

    /** 引擎类型 */
    EngineType getType();

    /** 校验任务配置是否合法 */
    boolean validate(TaskConfig config);

    /** 提交任务，返回 Job 句柄供后续状态追踪 */
    JobHandle submit(TaskConfig config);

    /** 停止指定 Job */
    void stop(JobHandle handle);

    /** 查询 Job 运行状态 */
    JobStatus getStatus(JobHandle handle);

    /** 获取 Job 运行指标 */
    Metrics getMetrics(JobHandle handle);
}
