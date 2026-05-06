package com.relake.job.agent.model;

import lombok.Data;

/**
 * Job 提交响应
 */
@Data
public class JobSubmitResponse {

    /** Agent 生成的 Job ID */
    private String jobId;

    /** 初始状态 */
    private String status;
}
