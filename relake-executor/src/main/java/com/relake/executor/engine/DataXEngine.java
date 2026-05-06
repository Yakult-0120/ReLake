package com.relake.executor.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relake.executor.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataX 引擎 — 离线批量同步
 * <p>
 * 生成 DataX JSON 任务配置文件，支持 MySQL Reader → 目标 Writer。
 * Phase 7: ProcessBuilder 执行 DataX Python 入口 + 进程状态跟踪。
 */
@Slf4j
@Component
public class DataXEngine implements SyncEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, JobHandle> jobRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Process> processRegistry = new ConcurrentHashMap<>();

    private final String dataxHome;

    public DataXEngine(@Value("${datax.home:/opt/datax}") String dataxHome) {
        this.dataxHome = dataxHome;
    }

    @Override
    public EngineType getType() {
        return EngineType.DATAX;
    }

    @Override
    public boolean validate(TaskConfig config) {
        if (config.getDatasourceHost() == null || config.getDatasourceHost().isBlank()) {
            log.warn("DataX引擎校验失败: 数据源主机为空");
            return false;
        }
        if (config.getSourceTables() == null || config.getSourceTables().isEmpty()) {
            log.warn("DataX引擎校验失败: 源表列表为空");
            return false;
        }
        log.info("DataX引擎校验通过: task={}, tables={}", config.getTaskId(), config.getSourceTables());
        return true;
    }

    @Override
    public JobHandle submit(TaskConfig config) {
        String jobId = "datax-" + config.getTaskId() + "-" + System.currentTimeMillis();
        JobHandle handle = JobHandle.of(EngineType.DATAX, jobId);

        try {
            String jobJson = generateDataXJson(config);
            log.info("DataX Job JSON 已生成: taskId={}, tables={}", config.getTaskId(), config.getSourceTables());

            // 写入临时 JSON 文件
            Path tempFile = Files.createTempFile("datax-job-", ".json");
            Files.writeString(tempFile, jobJson);
            log.info("DataX 配置已写入临时文件: {}", tempFile.toAbsolutePath());

            // 启动 DataX 进程
            File dataxDir = new File(dataxHome);
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder("python", dataxHome + "/bin/datax.py", tempFile.toAbsolutePath().toString());
            } else {
                pb = new ProcessBuilder("python", "bin/datax.py", tempFile.toAbsolutePath().toString());
            }
            pb.directory(dataxDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            processRegistry.put(jobId, process);

            // 守护线程: 读取 stdout/stderr 并等待进程结束
            Thread monitor = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[DataX] {} | {}", jobId, line);
                    }
                } catch (IOException e) {
                    log.warn("[DataX] 读取进程输出异常: jobId={}, error={}", jobId, e.getMessage());
                }

                try {
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        handle.setStatus(JobStatus.FINISHED);
                        log.info("[DataX] 任务完成: jobId={}, exitCode=0", jobId);
                    } else {
                        handle.setStatus(JobStatus.FAILED);
                        log.error("[DataX] 任务失败: jobId={}, exitCode={}", jobId, exitCode);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handle.setStatus(JobStatus.STOPPED);
                } finally {
                    processRegistry.remove(jobId);
                    // 清理临时文件
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ignored) {
                    }
                }
            }, "datax-monitor-" + jobId);
            monitor.setDaemon(true);
            monitor.start();

            handle.setStatus(JobStatus.RUNNING);
            log.info("DataX 任务已提交: jobId={}, taskId={}, dataxHome={}", jobId, config.getTaskId(), dataxHome);
        } catch (Exception e) {
            log.error("DataX 任务提交失败: taskId={}, error={}", config.getTaskId(), e.getMessage(), e);
            handle.setStatus(JobStatus.FAILED);
        }

        jobRegistry.put(jobId, handle);
        return handle;
    }

    @Override
    public void stop(JobHandle handle) {
        Process process = processRegistry.remove(handle.getJobId());
        if (process != null && process.isAlive()) {
            process.destroy();
            log.info("[DataX] 进程已终止: jobId={}", handle.getJobId());
        }
        JobHandle existing = jobRegistry.remove(handle.getJobId());
        if (existing != null) {
            existing.setStatus(JobStatus.STOPPED);
            log.info("DataX Job 已停止: jobId={}", handle.getJobId());
        }
    }

    @Override
    public JobStatus getStatus(JobHandle handle) {
        JobHandle existing = jobRegistry.get(handle.getJobId());
        return existing != null ? existing.getStatus() : JobStatus.UNKNOWN;
    }

    @Override
    public Metrics getMetrics(JobHandle handle) {
        return Metrics.empty();
    }

    // ──────── DataX JSON 配置生成 ────────

    private String generateDataXJson(TaskConfig config) throws IOException {
        Map<String, Object> job = new LinkedHashMap<>();

        // ── Reader ──
        Map<String, Object> reader = new LinkedHashMap<>();
        reader.put("name", "mysqlreader");
        Map<String, Object> readerParam = new LinkedHashMap<>();
        readerParam.put("username", config.getDatasourceUsername());
        readerParam.put("password", config.getDatasourcePassword());
        readerParam.put("column", List.of("*"));

        Map<String, Object> connection = new LinkedHashMap<>();
        connection.put("jdbcUrl", List.of(
                "jdbc:mysql://" + config.getDatasourceHost() + ":" + config.getDatasourcePort()
                        + "/" + config.getDatasourceDbName()
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        ));
        connection.put("table", config.getSourceTables());
        readerParam.put("connection", List.of(connection));
        reader.put("parameter", readerParam);

        // ── Writer ──
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "streamwriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();
        writerParam.put("print", true);
        writer.put("parameter", writerParam);

        // ── Job ──
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader);
        content.put("writer", writer);

        Map<String, Object> setting = new LinkedHashMap<>();
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", 3);
        speed.put("bytes", -1L);
        setting.put("speed", speed);
        setting.put("errorLimit", Map.of("record", 0, "percentage", 0.02));

        job.put("job", Map.of(
                "content", List.of(content),
                "setting", setting
        ));

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(job);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
