package com.relake.executor.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relake.common.web.BusinessException;
import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.executor.dto.JobStatusVO;
import com.relake.executor.dto.JobSubmitRequest;
import com.relake.executor.dto.JobSubmitResponse;
import com.relake.executor.feign.JobAgentClient;
import com.relake.executor.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataX 引擎 — 离线批量同步
 * <p>
 * 生成 DataX JSON 任务配置，通过 Job Agent（通用 CLI 执行代理）提交执行。
 * 自身不再直接调用 ProcessBuilder，解耦 DataX 运行环境。
 */
@Slf4j
@Component
public class DataXEngine implements SyncEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, JobHandle> jobRegistry = new ConcurrentHashMap<>();

    private final JobAgentClient agentClient;
    private final String dataxHome;
    private final String dockerHost;

    public DataXEngine(JobAgentClient agentClient,
                       @Value("${datax.home:/opt/datax}") String dataxHome,
                       @Value("${datax.docker-host:host.docker.internal}") String dockerHost) {
        this.agentClient = agentClient;
        this.dataxHome = dataxHome;
        this.dockerHost = dockerHost;
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
            // 生成 DataX JSON 配置文件内容
            String jobJson = generateDataXJson(config);
            String configFileName = "datax-job-" + config.getTaskId() + ".json";
            log.info("DataX JSON 配置已生成: taskId={}, tables={}", config.getTaskId(), config.getSourceTables());

            // 构造通用 Job 提交请求
            JobSubmitRequest req = new JobSubmitRequest();
            req.setTaskId(config.getTaskId());
            req.setTaskName(config.getTaskName());
            req.setCommand("python");
            req.setArgs(List.of("bin/datax.py", configFileName));
            req.setWorkingDir(dataxHome);
            req.setConfigFiles(Map.of(configFileName, jobJson));

            // 通过 Feign 调 Job Agent 提交
            R<JobSubmitResponse> resp = agentClient.submitJob(req);
            if (!resp.isSuccess() || resp.getData() == null) {
                throw new BusinessException(ResultCode.TASK_START_FAILED,
                        "Job Agent 提交失败: " + resp.getMessage());
            }

            handle.setStatus(JobStatus.RUNNING);
            handle.setInternalId(resp.getData().getJobId());
            jobRegistry.put(jobId, handle);
            log.info("DataX 任务已提交至 Job Agent: jobId={}, agentJobId={}, taskId={}",
                    jobId, resp.getData().getJobId(), config.getTaskId());

        } catch (BusinessException e) {
            log.error("DataX 任务提交失败: taskId={}, error={}", config.getTaskId(), e.getMessage());
            handle.setStatus(JobStatus.FAILED);
            jobRegistry.put(jobId, handle);
            throw e;
        } catch (Exception e) {
            log.error("DataX 任务提交异常: taskId={}, error={}", config.getTaskId(), e.getMessage(), e);
            handle.setStatus(JobStatus.FAILED);
            jobRegistry.put(jobId, handle);
            throw new BusinessException(ResultCode.TASK_START_FAILED,
                    "DataX Agent 不可用: " + e.getMessage());
        }

        return handle;
    }

    @Override
    public void stop(JobHandle handle) {
        try {
            if (handle.getInternalId() != null) {
                agentClient.stopJob(handle.getInternalId());
            }
        } catch (Exception e) {
            log.warn("停止 DataX Job 异常: jobId={}, error={}", handle.getJobId(), e.getMessage());
        }
        JobHandle existing = jobRegistry.remove(handle.getJobId());
        if (existing != null) {
            existing.setStatus(JobStatus.STOPPED);
            log.info("DataX Job 已停止: jobId={}", handle.getJobId());
        }
    }

    @Override
    public JobStatus getStatus(JobHandle handle) {
        if (handle.getInternalId() == null) {
            JobHandle local = jobRegistry.get(handle.getJobId());
            return local != null ? local.getStatus() : JobStatus.UNKNOWN;
        }

        try {
            R<JobStatusVO> resp = agentClient.getJobStatus(handle.getInternalId());
            if (resp.isSuccess() && resp.getData() != null) {
                return mapStatus(resp.getData().getStatus());
            }
        } catch (Exception e) {
            log.debug("查询 DataX Job 状态异常: {}", e.getMessage());
        }
        return JobStatus.UNKNOWN;
    }

    @Override
    public Metrics getMetrics(JobHandle handle) {
        if (handle.getInternalId() == null) {
            return Metrics.empty();
        }

        try {
            R<JobStatusVO> resp = agentClient.getJobStatus(handle.getInternalId());
            if (resp.isSuccess() && resp.getData() != null) {
                JobStatusVO s = resp.getData();
                return new Metrics()
                        .setRecordsIn(s.getOutputLines())
                        .setRecordsOut(s.getOutputLines())
                        .setErrorCount(s.getErrorLines());
            }
        } catch (Exception e) {
            log.debug("查询 DataX Job 指标异常: {}", e.getMessage());
        }
        return Metrics.empty();
    }

    // ──────── DataX JSON 配置生成 ────────

    private String generateDataXJson(TaskConfig config) throws Exception {
        Map<String, Object> job = new LinkedHashMap<>();

        // Reader
        Map<String, Object> reader = new LinkedHashMap<>();
        reader.put("name", "mysqlreader");
        Map<String, Object> readerParam = new LinkedHashMap<>();
        readerParam.put("username", config.getDatasourceUsername());
        readerParam.put("password", config.getDatasourcePassword());
        readerParam.put("column", List.of("*"));

        // 解决容器内外网络视角差异：localhost → host.docker.internal
        String jdbcHost = config.getDatasourceHost();
        if ("localhost".equals(jdbcHost) || "127.0.0.1".equals(jdbcHost)) {
            jdbcHost = dockerHost;
        }

        Map<String, Object> connection = new LinkedHashMap<>();
        connection.put("jdbcUrl", List.of(
                "jdbc:mysql://" + jdbcHost + ":" + config.getDatasourcePort()
                        + "/" + config.getDatasourceDbName()
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        ));
        connection.put("table", config.getSourceTables());
        readerParam.put("connection", List.of(connection));
        reader.put("parameter", readerParam);

        // Writer
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "streamwriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();
        writerParam.put("print", true);
        writer.put("parameter", writerParam);

        // Content
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader);
        content.put("writer", writer);

        // Setting
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

    /**
     * 将 Job Agent 的状态字符串映射为 JobStatus 枚举
     */
    private static JobStatus mapStatus(String status) {
        if (status == null) return JobStatus.UNKNOWN;
        return switch (status.toUpperCase()) {
            case "SUBMITTED" -> JobStatus.SUBMITTED;
            case "RUNNING" -> JobStatus.RUNNING;
            case "FINISHED" -> JobStatus.FINISHED;
            case "FAILED" -> JobStatus.FAILED;
            case "STOPPED" -> JobStatus.STOPPED;
            default -> JobStatus.UNKNOWN;
        };
    }
}
