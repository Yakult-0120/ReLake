package com.relake.common.dto;

import lombok.Data;

/**
 * DataX 配置传输对象 — Integration 内部 API 返回给 Job Agent (XXL-JOB Executor)
 */
@Data
public class DataXConfigDTO {
    /** 执行命令，如 "python" */
    private String command;
    /** 命令参数，如 "bin/datax.py datax-job-123.json" */
    private String args;
    /** 工作目录，如 "/opt/datax" */
    private String workingDir;
    /** DataX JSON 任务配置内容 */
    private String jobJson;
}
