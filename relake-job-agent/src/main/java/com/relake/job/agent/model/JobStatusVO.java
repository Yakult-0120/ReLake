package com.relake.job.agent.model;

import lombok.Data;

/**
 * Job 状态响应 — 通用 CLI Job 的运行状态与指标
 */
@Data
public class JobStatusVO {

    /** Job ID */
    private String jobId;

    /** 运行状态: SUBMITTED / RUNNING / FINISHED / FAILED / STOPPED */
    private String status;

    /** 已输出行数（stdout 行数统计） */
    private long outputLines;

    /** 错误行数（stderr 行数统计） */
    private long errorLines;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 进程退出码（完成时） */
    private Integer exitCode;
}
