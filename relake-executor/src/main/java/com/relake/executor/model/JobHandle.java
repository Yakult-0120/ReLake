package com.relake.executor.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 引擎 Job 句柄 — 提交任务后返回，用于后续状态查询和停止
 */
@Data
@Accessors(chain = true)
public class JobHandle {

    /** 引擎类型 */
    private EngineType engineType;

    /** 引擎内部 Job ID */
    private String jobId;

    /** 当前运行状态 */
    private JobStatus status;

    /** 提交时间 */
    private LocalDateTime startTime;

    /** 引擎内部附加信息（如 Flink Job ID、Canal destination 等） */
    private String internalId;

    public static JobHandle of(EngineType engineType, String jobId) {
        JobHandle handle = new JobHandle();
        handle.setEngineType(engineType);
        handle.setJobId(jobId);
        handle.setStatus(JobStatus.SUBMITTED);
        handle.setStartTime(LocalDateTime.now());
        return handle;
    }
}
