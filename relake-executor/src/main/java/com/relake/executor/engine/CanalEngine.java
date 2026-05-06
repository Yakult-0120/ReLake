package com.relake.executor.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relake.executor.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canal 引擎 — MySQL binlog 实时 CDC 采集
 * <p>
 * Canal Server 已配置为 Kafka 模式（flatMessage=true），
 * 本引擎负责启动 KafkaConsumer 消费 Canal binlog 事件，
 * 解析 flat JSON 消息，记录 CDC 事件。
 * <p>
 * Phase 7: 完整的 KafkaConsumer 实现，真实消费、解析、指标统计。
 */
@Slf4j
@Component
public class CanalEngine implements SyncEngine {

    private final ConcurrentHashMap<String, JobHandle> jobRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConsumerThread> consumerThreads = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String bootstrapServers;
    private final String canalKafkaTopic;

    public CanalEngine(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${canal.kafka.topic:example}") String canalKafkaTopic) {
        this.bootstrapServers = bootstrapServers;
        this.canalKafkaTopic = canalKafkaTopic;
    }

    // ──────────────────── 内部类 ────────────────────

    /**
     * 消费者线程封装
     */
    private static class ConsumerThread {
        Thread thread;
        final KafkaConsumer<String, String> consumer;
        final AtomicBoolean running = new AtomicBoolean(true);
        final MetricsTracker metrics = new MetricsTracker();

        ConsumerThread(KafkaConsumer<String, String> consumer) {
            this.consumer = consumer;
        }
    }

    /**
     * 线程安全的运行指标累加器
     */
    static class MetricsTracker {
        volatile long recordsIn = 0;
        volatile long recordsOut = 0;
        volatile long bytesIn = 0;
        volatile long errorCount = 0;
    }

    // ──────────────────── SyncEngine 实现 ────────────────────

    @Override
    public EngineType getType() {
        return EngineType.CANAL;
    }

    @Override
    public boolean validate(TaskConfig config) {
        if (config.getDatasourceHost() == null || config.getDatasourceHost().isBlank()) {
            log.warn("Canal引擎校验失败: 数据源主机为空");
            return false;
        }
        if (config.getDatasourceDbName() == null || config.getDatasourceDbName().isBlank()) {
            log.warn("Canal引擎校验失败: 数据库名为空");
            return false;
        }
        if (config.getSourceTables() == null || config.getSourceTables().isEmpty()) {
            log.warn("Canal引擎校验失败: 源表列表为空");
            return false;
        }
        log.info("Canal引擎校验通过: task={}, db={}, tables={}",
                config.getTaskId(), config.getDatasourceDbName(), config.getSourceTables());
        return true;
    }

    @Override
    public JobHandle submit(TaskConfig config) {
        String jobId = "canal-" + config.getTaskId() + "-" + System.currentTimeMillis();
        JobHandle handle = JobHandle.of(EngineType.CANAL, jobId);

        // 构建 Kafka Consumer 配置
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "relake-canal-task-" + config.getTaskId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        ConsumerThread ct = new ConsumerThread(consumer);

        Thread thread = new Thread(() -> {
            try {
                consumer.subscribe(List.of(canalKafkaTopic));
                handle.setStatus(JobStatus.RUNNING);
                log.info("[CanalEngine] Consumer started: jobId={}, topic={}, bootstrap={}, tables={}",
                        jobId, canalKafkaTopic, bootstrapServers, config.getSourceTables());

                while (ct.running.get() && !Thread.currentThread().isInterrupted()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    ct.metrics.recordsIn += records.count();
                    for (ConsumerRecord<String, String> record : records) {
                        ct.metrics.bytesIn += record.value() != null ? record.value().length() : 0;
                        processCanalMessage(record.value(), config, ct.metrics);
                    }
                }
            } catch (WakeupException e) {
                log.info("[CanalEngine] Consumer wakeup: jobId={}", jobId);
            } catch (Exception e) {
                log.error("[CanalEngine] Consumer error: jobId={}, error={}", jobId, e.getMessage(), e);
                handle.setStatus(JobStatus.FAILED);
            } finally {
                consumer.close();
                log.info("[CanalEngine] Consumer closed: jobId={}", jobId);
                if (handle.getStatus() == JobStatus.RUNNING) {
                    handle.setStatus(JobStatus.STOPPED);
                }
            }
        }, "canal-consumer-" + jobId);
        thread.setDaemon(true);

        ct.thread = thread;
        thread.start();
        handle.setStatus(JobStatus.RUNNING);

        consumerThreads.put(jobId, ct);
        jobRegistry.put(jobId, handle);

        log.info("Canal 任务已提交: jobId={}, taskId={}, db={}, tables={}, topic={}",
                jobId, config.getTaskId(), config.getDatasourceDbName(), config.getSourceTables(), canalKafkaTopic);
        return handle;
    }

    @Override
    public void stop(JobHandle handle) {
        ConsumerThread ct = consumerThreads.remove(handle.getJobId());
        JobHandle existing = jobRegistry.remove(handle.getJobId());

        if (ct != null) {
            ct.running.set(false);
            ct.consumer.wakeup();
            log.info("[CanalEngine] Stopping consumer: jobId={}", handle.getJobId());
        }
        if (existing != null) {
            existing.setStatus(JobStatus.STOPPED);
            log.info("Canal Job 已停止: jobId={}", handle.getJobId());
        } else {
            log.warn("Canal Job 不存在: jobId={}", handle.getJobId());
        }
    }

    @Override
    public JobStatus getStatus(JobHandle handle) {
        JobHandle existing = jobRegistry.get(handle.getJobId());
        return existing != null ? existing.getStatus() : JobStatus.UNKNOWN;
    }

    @Override
    public Metrics getMetrics(JobHandle handle) {
        ConsumerThread ct = consumerThreads.get(handle.getJobId());
        if (ct == null) {
            return Metrics.empty();
        }
        MetricsTracker m = ct.metrics;
        return new Metrics()
                .setRecordsIn(m.recordsIn)
                .setRecordsOut(m.recordsOut)
                .setBytesIn(m.bytesIn)
                .setErrorCount(m.errorCount);
    }

    // ──────────────────── 消息解析 ────────────────────

    /**
     * 解析 Canal flat JSON 消息，过滤表并记录 CDC 事件。
     * <p>
     * Canal flat JSON 格式示例：
     * <pre>
     * { "database": "business_db", "table": "users", "type": "INSERT",
     *   "data": [{"id":5,"username":"e2e-test","email":"e2e@test.com"}],
     *   "old": null, "isDdl": false, "es": 1234567890, "ts": 1234567890 }
     * </pre>
     */
    private void processCanalMessage(String json, TaskConfig config, MetricsTracker metrics) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            JsonNode msg = objectMapper.readTree(json);
            if (msg == null || msg.isNull()) {
                return;
            }

            String database = msg.path("database").asText("");
            String table = msg.path("table").asText("");
            String type = msg.path("type").asText("");
            boolean isDdl = msg.path("isDdl").asBoolean(false);

            // 仅处理配置中指定的表
            if (config.getSourceTables() == null || !config.getSourceTables().contains(table)) {
                return;
            }

            // 跳过 DDL
            if (isDdl) {
                log.info("[Canal CDC] DDL {}.{} | sql={}", database, table,
                        msg.path("sql").asText(""));
                return;
            }

            JsonNode dataNode = msg.path("data");
            JsonNode oldNode = msg.path("old");

            if (!dataNode.isArray() || dataNode.size() == 0) {
                return;
            }

            // 遍历变更行
            for (JsonNode row : dataNode) {
                metrics.recordsOut++;

                StringBuilder fields = new StringBuilder();
                Iterator<String> it = row.fieldNames();
                while (it.hasNext()) {
                    String field = it.next();
                    if (fields.length() > 0) {
                        fields.append(", ");
                    }
                    fields.append(field).append("=").append(row.get(field));
                }

                switch (type) {
                    case "INSERT":
                        log.info("[Canal CDC] INSERT {}.{} | {}", database, table, fields);
                        break;
                    case "UPDATE":
                        log.info("[Canal CDC] UPDATE {}.{} | {}", database, table, fields);
                        // 如果有旧值，也记录关键信息
                        if (oldNode != null && oldNode.isArray() && oldNode.size() > 0) {
                            JsonNode oldRow = oldNode.get(0);
                            StringBuilder oldFields = new StringBuilder();
                            Iterator<String> oldIt = oldRow.fieldNames();
                            while (oldIt.hasNext()) {
                                String f = oldIt.next();
                                if (oldFields.length() > 0) {
                                    oldFields.append(", ");
                                }
                                oldFields.append(f).append("=").append(oldRow.get(f));
                            }
                            log.info("[Canal CDC] UPDATE old {}.{} | {}", database, table, oldFields);
                        }
                        break;
                    case "DELETE":
                        log.info("[Canal CDC] DELETE {}.{} | {}", database, table, fields);
                        break;
                    default:
                        log.debug("[Canal CDC] {} {}.{} | {}", type, database, table, fields);
                }
            }
        } catch (Exception e) {
            metrics.errorCount++;
            log.warn("[CanalEngine] Failed to parse Canal message: {}", e.getMessage());
        }
    }
}
