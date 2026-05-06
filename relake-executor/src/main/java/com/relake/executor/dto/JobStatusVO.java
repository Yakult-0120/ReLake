package com.relake.executor.dto;

import lombok.Data;

/**
 * Job 状态响应 — 通用 CLI Job 的运行状态
 */
@Data
public class JobStatusVO {

    private String jobId;
    private String status;
    private long outputLines;
    private long errorLines;
    private String errorMessage;
    private Integer exitCode;
}
