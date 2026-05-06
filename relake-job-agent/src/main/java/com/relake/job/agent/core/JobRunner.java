package com.relake.job.agent.core;

import com.relake.job.agent.model.JobStatusVO;
import com.relake.job.agent.model.JobSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 Job 执行器 — ProcessBuilder 执行任意 CLI 命令 + 守护线程监控
 * <p>
 * 不绑定特定工具（DataX、Sqoop、Kettle 等均可）。
 * 命令格式、参数、工作目录均由请求传入。
 */
@Slf4j
@Component
public class JobRunner {

    private final JobRegistry registry;
    private final ConcurrentHashMap<String, Process> processMap = new ConcurrentHashMap<>();

    public JobRunner(JobRegistry registry) {
        this.registry = registry;
    }

    /**
     * 提交 Job：写配置文件 → ProcessBuilder 启动进程 → 守护线程监控
     */
    public void submit(String jobId, JobSubmitRequest request) {
        JobStatusVO status = new JobStatusVO();
        status.setJobId(jobId);
        status.setStatus("SUBMITTED");
        registry.register(jobId, status);

        try {
            // 确定工作目录
            String workDir = request.getWorkingDir();
            if (workDir == null || workDir.isBlank()) {
                workDir = System.getProperty("java.io.tmpdir");
            }
            Path workPath = Paths.get(workDir);

            // 写入配置文件（如 DataX JSON）
            if (request.getConfigFiles() != null && !request.getConfigFiles().isEmpty()) {
                for (Map.Entry<String, String> entry : request.getConfigFiles().entrySet()) {
                    Path filePath = workPath.resolve(entry.getKey());
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, entry.getValue());
                    log.info("[Job Agent] 配置文件已写入: {}", filePath.toAbsolutePath());
                }
            }

            // 构造命令
            List<String> cmd = new ArrayList<>();
            cmd.add(request.getCommand());
            if (request.getArgs() != null) {
                cmd.addAll(request.getArgs());
            }

            log.info("[Job Agent] 执行命令: {} (workDir={})", String.join(" ", cmd), workDir);

            // 启动进程
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workPath.toFile());
            pb.redirectErrorStream(true);

            // 设置环境变量
            if (request.getEnv() != null && !request.getEnv().isEmpty()) {
                pb.environment().putAll(request.getEnv());
            }

            Process process = pb.start();
            processMap.put(jobId, process);
            status.setStatus("RUNNING");
            log.info("[Job Agent] 进程已启动: jobId={}, taskId={}, pid={}",
                    jobId, request.getTaskId(), process.pid());

            // 守护线程
            Thread monitor = new Thread(
                    () -> monitorProcess(jobId, process, request.getConfigFiles(), workPath),
                    "job-monitor-" + jobId);
            monitor.setDaemon(true);
            monitor.start();

        } catch (Exception e) {
            log.error("[Job Agent] 提交失败: jobId={}, error={}", jobId, e.getMessage(), e);
            status.setStatus("FAILED");
            status.setErrorMessage("提交失败: " + e.getMessage());
        }
    }

    /**
     * 停止 Job：强制终止进程
     */
    public boolean stop(String jobId) {
        Process process = processMap.remove(jobId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            registry.update(jobId, "STOPPED", "手动停止", null);
            log.info("[Job Agent] 进程已终止: jobId={}", jobId);
            return true;
        }
        log.warn("[Job Agent] 进程不存在或已结束: jobId={}", jobId);
        return false;
    }

    // ──────── 内部方法 ────────

    private void monitorProcess(String jobId, Process process,
                                 Map<String, String> configFiles, Path workPath) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[{}] {}", jobId, line);
                registry.incrementOutputLines(jobId, 1);
            }
        } catch (IOException e) {
            log.warn("[Job Agent] 读取进程输出异常: jobId={}, error={}", jobId, e.getMessage());
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                registry.update(jobId, "FINISHED", null, 0);
                log.info("[Job Agent] 任务完成: jobId={}, exitCode=0", jobId);
            } else {
                registry.update(jobId, "FAILED",
                        "进程异常退出，exitCode=" + exitCode, exitCode);
                log.error("[Job Agent] 任务失败: jobId={}, exitCode={}", jobId, exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registry.update(jobId, "STOPPED", "监控线程被中断", null);
        } finally {
            processMap.remove(jobId);
            // 清理配置文件
            if (configFiles != null) {
                for (String filename : configFiles.keySet()) {
                    try {
                        Files.deleteIfExists(workPath.resolve(filename));
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }
}
