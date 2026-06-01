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

import java.util.ArrayList;
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
            // 1. 调度失败（如执行器离线、地址为空）→ 直接 FAILED
            if (logDTO.getTriggerCode() == 500) {
                log.warn("DataX getStatus: 调度失败, xxlJobId={}, triggerMsg={}",
                        xxlJobId, logDTO.getTriggerMsg());
                return JobStatus.FAILED;
            }
            // 2. 调度成功，检查执行结果
            JobStatus status = mapHandleCode(logDTO.getHandleCode());
            log.info("DataX getStatus: xxlJobId={}, triggerCode={}, handleCode={}, handleMsg={}, mappedStatus={}",
                    xxlJobId, logDTO.getTriggerCode(), logDTO.getHandleCode(), logDTO.getHandleMsg(), status);
            return status;
        } catch (Exception e) {
            log.error("DataX getStatus 异常: internalId={}, error={}", handle.getInternalId(), e.getMessage(), e);
        }
        return JobStatus.UNKNOWN;
    }

    @Override
    public String getLog(JobHandle handle) {
        if (handle.getInternalId() == null) return null;
        try {
            int xxlJobId = Integer.parseInt(handle.getInternalId());
            Integer logId = adminClient.getLastLogId(xxlJobId);
            if (logId == null) return null;
            String raw = adminClient.getJobLogContent(logId);
            // 统一 Windows \r\n → \n，避免前端渲染异常
            return raw != null ? raw.replace("\r\n", "\n") : null;
        } catch (Exception e) {
            log.warn("查询 DataX Job 日志异常: jobId={}, error={}", handle.getJobId(), e.getMessage());
            return null;
        }
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
        Map<String, Object> reader = buildReader(config);

        // Writer — 根据目标存储类型选择
        Map<String, Object> writer = buildWriter(config);

        // Content
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader);
        content.put("writer", writer);

        // Setting
        Map<String, Object> setting = buildSetting();

        job.put("job", Map.of(
                "content", List.of(content),
                "setting", setting
        ));

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(job);
    }

    /**
     * 构建 DataX Reader — 目前统一使用 mysqlreader
     */
    private Map<String, Object> buildReader(TaskConfig config) {
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
        return reader;
    }

    /**
     * 根据目标存储类型构建 DataX Writer
     */
    private Map<String, Object> buildWriter(TaskConfig config) {
        String storageType = config.getTargetStorageType();
        if (storageType == null || storageType.isBlank()) {
            log.warn("目标存储类型为空，使用 streamwriter（仅打印到标准输出）");
            return buildStreamWriter();
        }

        return switch (storageType.toUpperCase()) {
            case "MINIO", "S3" -> buildS3Writer(config);
            case "HDFS" -> buildHdfsWriter(config);
            case "FILE" -> buildFileWriter(config);
            case "LOCAL" -> buildTxtFileWriter(config);
            case "KAFKA" -> {
                log.warn("KAFKA 类型目标存储暂不支持 DataX 同步，任务将仅打印到标准输出");
                yield buildStreamWriter();
            }
            default -> {
                log.warn("未知的目标存储类型: {}，使用 streamwriter（仅打印到标准输出）", storageType);
                yield buildStreamWriter();
            }
        };
    }

    /**
     * MinIO/S3 Writer — DataX 先写本地 staging，完成后由 Job Agent 通过 mc 上传至 MinIO
     * <p>
     * 不需要 hadoop-aws 依赖，更稳定可靠。
     * staging 路径：/opt/datax/output/{taskId}/
     */
    private Map<String, Object> buildS3Writer(TaskConfig config) {
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "txtfilewriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();

        // staging 路径供 mc 上传使用
        String stagingPath = "/opt/datax/output/" + config.getTaskId();
        writerParam.put("path", stagingPath);
        writerParam.put("fileName", config.getTaskName());
        writerParam.put("writeMode", "truncate");
        writerParam.put("fieldDelimiter", "\t");
        writerParam.put("encoding", "UTF-8");
        writer.put("parameter", writerParam);

        log.info("DataX Writer(MinIO staging): path={}, bucket={}, tables={}",
                stagingPath, config.getTargetBucket(), config.getSourceTables());
        return writer;
    }

    /**
     * FILE Writer — 写入文件服务器指定路径
     * <p>
     * 使用 txtfilewriter 写入本地/挂载路径，路径由 endpoint(服务器IP) + bucket(目录) 组合。
     * 路径优先级：configJson.outputPath > bucket > 默认 /opt/datax/output/{taskId}
     */
    private Map<String, Object> buildFileWriter(TaskConfig config) {
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "txtfilewriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();

        // 路径：优先 configJson，其次用 bucket（目标路径），最后默认
        String outputPath = parseOutputPath(config);
        if (outputPath == null) {
            String bucket = config.getTargetBucket();
            if (bucket != null && !bucket.isBlank()) {
                outputPath = bucket.endsWith("/") ? bucket + config.getTaskId() : bucket + "/" + config.getTaskId();
            } else {
                outputPath = "/opt/datax/output/" + config.getTaskId();
            }
        }

        writerParam.put("path", outputPath);
        writerParam.put("fileName", config.getTaskName());
        writerParam.put("writeMode", "truncate");
        writerParam.put("fieldDelimiter", "\t");
        writerParam.put("encoding", "UTF-8");
        writer.put("parameter", writerParam);

        log.info("DataX Writer(FILE → txtfilewriter): server={}, path={}, tables={}",
                config.getTargetEndpoint(), outputPath, config.getSourceTables());
        return writer;
    }

    /**
     * txtfilewriter — 写入本地文件（仅用于 LOCAL 目标存储）
     */
    private Map<String, Object> buildTxtFileWriter(TaskConfig config) {
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "txtfilewriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();
        String outputPath = parseOutputPath(config);
        if (outputPath == null) {
            outputPath = "/opt/datax/output/" + config.getTaskId();
        }
        writerParam.put("path", outputPath);
        writerParam.put("fileName", config.getTaskName());
        writerParam.put("writeMode", "truncate");
        writerParam.put("fieldDelimiter", "\t");
        writerParam.put("encoding", "UTF-8");
        writer.put("parameter", writerParam);

        log.info("DataX Writer(txtfilewriter): path={}, tables={}", outputPath, config.getSourceTables());
        return writer;
    }

    /**
     * 从 configJson 中解析自定义 outputPath，形如 {"outputPath": "/data/output"}
     */
    @SuppressWarnings("unchecked")
    private String parseOutputPath(TaskConfig config) {
        String configJson = config.getConfigJson();
        if (configJson == null || configJson.isBlank()) return null;
        try {
            Map<String, Object> custom = objectMapper.readValue(configJson, Map.class);
            Object path = custom.get("outputPath");
            return path != null ? path.toString() : null;
        } catch (Exception e) {
            log.debug("解析 configJson 中的 outputPath 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 ColumnMeta 列表转为 hdfswriter 所需的 column 定义
     */
    private List<Map<String, String>> buildColumnDefs(TaskConfig config) {
        List<Map<String, String>> columns = new ArrayList<>();
        if (config.getSourceColumns() != null && !config.getSourceColumns().isEmpty()) {
            for (ColumnMeta col : config.getSourceColumns()) {
                columns.add(Map.of("name", col.getName(), "type", col.getType()));
            }
        }
        return columns;
    }

    /**
     * hdfswriter — 写入 HDFS
     */
    private Map<String, Object> buildHdfsWriter(TaskConfig config) {
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "hdfswriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();
        writerParam.put("defaultFS", config.getTargetEndpoint());
        String hdfsPath = "/" + (config.getTargetBucket() != null ? config.getTargetBucket() : "relake");
        writerParam.put("path", hdfsPath);
        writerParam.put("fileName", config.getTaskName());
        writerParam.put("fileType", "orc");
        writerParam.put("compress", "NONE");
        writerParam.put("fieldDelimiter", "\u0001");
        writerParam.put("haveKerberos", false);
        writerParam.put("encoding", "UTF-8");
        writerParam.put("writeMode", "append");
        if (config.getSourceColumns() != null && !config.getSourceColumns().isEmpty()) {
            writerParam.put("column", buildColumnDefs(config));
        }
        writer.put("parameter", writerParam);

        log.info("DataX Writer(hdfswriter): defaultFS={}, path={}, tables={}",
                config.getTargetEndpoint(), hdfsPath, config.getSourceTables());
        return writer;
    }

    /**
     * streamwriter — 打印到标准输出（调试/无目标存储时使用）
     */
    private Map<String, Object> buildStreamWriter() {
        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", "streamwriter");
        Map<String, Object> writerParam = new LinkedHashMap<>();
        writerParam.put("print", true);
        writer.put("parameter", writerParam);
        return writer;
    }

    private Map<String, Object> buildSetting() {
        Map<String, Object> setting = new LinkedHashMap<>();
        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", 3);
        speed.put("bytes", -1L);
        setting.put("speed", speed);
        setting.put("errorLimit", Map.of("record", 0, "percentage", 0.02));
        return setting;
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
