package com.relake.job.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 通用 Job 提交请求
 * <p>
 * 不绑定任何特定工具。executor 端负责将引擎任务翻译为命令行调用。
 */
@Data
public class JobSubmitRequest {

    /** 关联的同步任务 ID（用于日志关联） */
    private Long taskId;

    /** 任务名称（用于日志可读性） */
    private String taskName;

    /** 可执行命令，如 python / sqoop / kettle */
    private String command;

    /** 命令参数 */
    private List<String> args;

    /** 工作目录（可选，默认 /tmp） */
    private String workingDir;

    /** 环境变量（可选） */
    private Map<String, String> env;

    /**
     * 配置文件映射：文件名 → 内容
     * Agent 在 workDir 下创建这些文件后执行命令，执行完毕自动清理。
     * 典型场景：DataX 的 JSON 配置文件。
     */
    private Map<String, String> configFiles;
}
