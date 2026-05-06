package com.relake.executor.dto;

import lombok.Data;

/**
 * Job 提交响应
 */
@Data
public class JobSubmitResponse {

    private String jobId;
    private String status;
}
