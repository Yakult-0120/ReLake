package com.relake.executor.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.executor.client.XxlJobAdminClient;
import com.relake.executor.dto.XxlJobLogDTO;
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
 * 通过 XXL-JOB Admin API 创建任务、触发执行、查询状态和日志。
 * Job Agent (XXL-JOB Executor) 负责实际的 DataX 命令执行。
 */
@Slf4j
@Component
public class DataXEngine implements SyncEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Long, Integer> taskJobMap = new ConcurrentHashMap<>();

    private final XxlJobAdminClient adminClient;
    private final String dataxHome;

    public DataXEngine(XxlJobAdminClient adminClient,
                       @Value("${datax.home:/opt/datax}") String dataxHome) {
        this.adminClient = adminClient;
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
        Long taskId = config.getTaskId();
        JobHandle handle = JobHandle.of(EngineType.DATAX, "xxl-" + taskId);

        try {
            // 1. 创建或复用 XXL-JOB 任务
            Integer xxlJobId = taskJobMap.get(taskId);
            if (xxlJobId == null) {
                xxlJobId = adminClient.findOrCreateJob(
                        "DataX-" + config.getTaskName(),
                        "dataxSync",
                        String.valueOf(taskId),        // executorParam = taskId
                        "0 0 2 * * ?"                  // 默认凌晨2点 Cron
                );
                taskJobMap.put(taskId, xxlJobId);
            }

            // 2. 触发执行
            adminClient.triggerJob(xxlJobId, String.valueOf(taskId));

            // 3. 返回 JobHandle
            handle.setStatus(JobStatus.RUNNING);
            handle.setInternalId(String.valueOf(xxlJobId));
            log.info("DataX 任务已通过 XXL-JOB 触发: taskId={}, xxlJobId={}", taskId, xxlJobId);

        } catch (BusinessException e) {
            log.error("DataX 任务提交失败: taskId={}, error={}", config.getTaskId(), e.getMessage());
            handle.setStatus(JobStatus.FAILED);
            throw e;
        } catch (Exception e) {
            log.error("DataX 任务提交异常: taskId={}, error={}", config.getTaskId(), e.getMessage(), e);
            handle.setStatus(JobStatus.FAILED);
            throw new BusinessException(ResultCode.TASK_START_FAILED,
                    "XXL-JOB 触发失败: " + e.getMessage());
        }

        return handle;
    }

    @Override
    public void stop(JobHandle handle) {
        try {
            if (handle.getInternalId() != null) {
                int xxlJobId = Integer.parseInt(handle.getInternalId());
                adminClient.killJob(xxlJobId);
            }
        } catch (Exception e) {
            log.warn("停止 DataX Job 异常: jobId={}, error={}", handle.getJobId(), e.getMessage());
        }
        log.info("DataX Job 已请求停止: jobId={}", handle.getJobId());
    }

    @Override
    public JobStatus getStatus(JobHandle handle) {
        if (handle.getInternalId() == null) {
            log.warn("DataX getStatus: internalId 为空, jobId={}", handle.getJobId());
            return JobStatus.UNKNOWN;
        }

        try {
            int xxlJobId = Integer.parseInt(handle.getInternalId());
            log.info("DataX getStatus: 查询 xxlJobId={}, jobId={}", xxlJobId, handle.getJobId());
            XxlJobLogDTO logDTO = adminClient.getLastLog(xxlJobId);
            if (logDTO == null) {
                log.warn("DataX getStatus: getLastLog 返回 null, xxlJobId={}", xxlJobId);
                return JobStatus.UNKNOWN;
            }
            JobStatus status = mapHandleCode(logDTO.getHandleCode());
            log.info("DataX getStatus: xxlJobId={}, handleCode={}, handleMsg={}, mappedStatus={}",
                    xxlJobId, logDTO.getHandleCode(), logDTO.getHandleMsg(), status);
            return status;
        } catch (Exception e) {
            log.error("DataX getStatus 异常: internalId={}, error={}", handle.getInternalId(), e.getMessage(), e);
        }
        return JobStatus.UNKNOWN;
    }

    @Override
    public Metrics getMetrics(JobHandle handle) {
        if (handle.getInternalId() == null) {
            return Metrics.empty();
        }

        try {
            int xxlJobId = Integer.parseInt(handle.getInternalId());
            Integer logId = adminClient.getLastLogId(xxlJobId);
            if (logId == null) return Metrics.empty();

            String logContent = adminClient.getJobLogContent(logId);
            if (logContent == null) return Metrics.empty();

            return parseMetricsFromLog(logContent);
        } catch (Exception e) {
            log.debug("查询 DataX Job 指标异常: {}", e.getMessage());
        }
        return Metrics.empty();
    }

    // ──────── DataX JSON 配置生成 ────────

    /**
     * 根据 TaskConfig 生成 DataX JSON 任务配置
     * <p>
     * （供 Integration 内部 API 调用，返回给 Job Agent 执行）
     */
    public String buildDataXJson(TaskConfig config) {
        try {
            return generateDataXJson(config);
        } catch (Exception e) {
            log.error("生成 DataX JSON 失败: taskId={}, error={}", config.getTaskId(), e.getMessage());
            throw new RuntimeException("生成 DataX JSON 失败", e);
        }
    }

    private String generateDataXJson(TaskConfig config) throws Exception {
        Map<String, Object> job = new LinkedHashMap<>();

        // Reader
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

    // ──────── 状态映射 ────────

    /**
     * 将 XXL-JOB handle_code 映射为 JobStatus
     * 200 = 成功, 500 = 失败, 0 = 运行中
     */
    private static JobStatus mapHandleCode(int handleCode) {
        return switch (handleCode) {
            case 200 -> JobStatus.FINISHED;
            case 500 -> JobStatus.FAILED;
            default -> JobStatus.RUNNING;
        };
    }

    /**
     * 从 XXL-JOB 执行日志中提取 DataX 指标
     */
    private static Metrics parseMetricsFromLog(String logContent) {
        if (logContent == null || logContent.isBlank()) return Metrics.empty();
        Metrics metrics = new Metrics();

        try {
            String[] lines = logContent.split("\n");
            for (String line : lines) {
                if (line.contains("读出记录总数")) {
                    metrics.setRecordsOut(extractNumber(line));
                } else if (line.contains("读写失败总数")) {
                    metrics.setErrorCount(extractNumber(line));
                } else if (line.contains("记录写入速度")) {
                    metrics.setRecordsIn(extractNumber(line));
                }
            }
        } catch (Exception ignored) {
        }
        return metrics;
    }

    private static long extractNumber(String line) {
        try {
            String num = line.replaceAll("[^0-9]", "");
            return num.isEmpty() ? 0 : Long.parseLong(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
