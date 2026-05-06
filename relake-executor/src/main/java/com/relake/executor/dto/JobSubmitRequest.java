package com.relake.executor.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通用 Job 提交请求 — executor 发往 Job Agent
 */
@Data
public class JobSubmitRequest {

    private Long taskId;
    private String taskName;
    private String command;
    private List<String> args;
    private String workingDir;
    private Map<String, String> env;
    private Map<String, String> configFiles;
}
