# relake-executor

ReLake 任务执行器 —— 基于策略模式的多引擎 CDC 实现层。

## 定位

`relake-executor` 是 ReLake 的**引擎执行层**，定义统一的 `SyncEngine` 接口，并为 Canal、Flink CDC、DataX 三种采集引擎提供具体实现。该模块不直接操作数据库，不持有任务状态，仅负责引擎级别的 Job 生命周期管理。

## 文件结构

```
relake-executor/src/main/java/com/relake/executor/
├── ExecutorApplication.java          # 启动类（端口 8084）
├── model/
│   ├── EngineType.java               # 引擎类型枚举：CANAL / FLINK_CDC / DATAX
│   ├── JobStatus.java                # Job 状态：SUBMITTED → RUNNING → FINISHED / FAILED / STOPPED
│   ├── JobHandle.java                # Job 句柄（engineType + jobId + status + startTime）
│   ├── Metrics.java                  # 运行指标（recordsIn/Out, bytesIn/Out, latencyMs）
│   └── TaskConfig.java               # 引擎入参（datasource/target 完整字段 + sourceTables）
├── engine/
│   ├── SyncEngine.java               # 【核心】引擎抽象接口（策略模式）
│   ├── SyncEngineFactory.java        # 引擎工厂：@PostConstruct 自动注册所有 Bean
│   ├── CanalEngine.java              # Canal 引擎实现（Kafka 消费骨架）
│   ├── FlinkCdcEngine.java           # Flink CDC 引擎实现（SQL Gateway REST 客户端）
│   └── DataXEngine.java              # DataX 引擎实现（JSON 配置生成）
├── config/
│   └── OkHttpConfig.java             # OkHttpClient Bean（FlinkCdcEngine 使用）
└── controller/
    └── JobController.java            # REST: /api/v1/jobs/{jobId} 查询引擎 Job
```

## 核心设计

### 1. 策略模式 — SyncEngine 接口

```
                    ┌──────────────────┐
                    │   SyncEngine      │
                    │   (接口)           │
                    ├──────────────────┤
                    │ + getType()       │ —— EngineType
                    │ + validate()      │ —— 校验 TaskConfig
                    │ + submit()        │ —— 提交任务 → JobHandle
                    │ + stop()          │ —— 停止任务
                    │ + getStatus()     │ —— 查询状态
                    │ + getMetrics()    │ —— 获取指标
                    └────────┬─────────┘
                             │ Implements
            ┌────────────────┼─────────────────┐
            ▼                ▼                  ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────────┐
    │ CanalEngine  │  │FlinkCdcEngine│  │ DataXEngine  │
    │ @Component   │  │ @Component   │  │ @Component   │
    │ CANAL        │  │ FLINK_CDC    │  │ DATAX        │
    └─────────────┘  └──────────────┘  └──────────────┘
```

每种引擎加 `@Component` 注解后，`SyncEngineFactory` 在启动时通过 `@PostConstruct` 自动发现并注册到 `Map<EngineType, SyncEngine>`，上层调用方只需传入 `EngineType` 即可路由。

### 2. 工厂自动注册

```java
@Component
@RequiredArgsConstructor
public class SyncEngineFactory {

    private final List<SyncEngine> engines;   // Spring 自动注入所有实现

    @PostConstruct
    public void init() {
        engineMap = engines.stream()
                .collect(Collectors.toMap(SyncEngine::getType, e -> e));
    }

    public SyncEngine getEngine(EngineType type) { ... }
}
```

> **新增引擎只需两步**：1. 实现 `SyncEngine` 接口；2. 加 `@Component` 注解。工厂会自动识别。

### 3. 引擎实现对比

| 维度 | CanalEngine | FlinkCdcEngine | DataXEngine |
|------|------------|----------------|-------------|
| **采集方式** | MySQL binlog 实时 | 全量 + 增量（CDC） | 离线批量 |
| **中间件** | Kafka 消费 | Flink SQL Gateway | 进程直连 |
| **输出** | Paimon Sink | Flink Paimon Sink | 文件/目标存储 |
| **Phase 5 状态** | 骨架（Job 跟踪） | REST 客户端已实现 | JSON 配置生成 |
| **Phase 7 增强** | Kafka Consumer 启动 | 实际提交 Flink Job | ProcessBuilder 执行 DataX |
| **Job 跟踪** | ConcurrentHashMap | ConcurrentHashMap | ConcurrentHashMap |
| **依赖** | canal.client + spring-kafka | OkHttp | Jackson |

### 4. Job 生命周期

```
submit(TaskConfig)
      │
      ▼
  JobHandle ──→ SUBMITTED ──→ RUNNING
      │            │              │
      │            ▼              ▼
      │          FAILED        FINISHED
      │                           │
      └── stop() ──→ STOPPED ←───┘
```

Job 状态通过 ConcurrentHashMap 在进程内跟踪，Phase 7+ 可扩展为 Redis 共享状态。

### 5. 引擎入参 TaskConfig

引擎不直接依赖 `relake-metadata` 服务。调用方（Integration 服务）通过 Feign 从 metadata 获取解密后的数据源/目标信息，组装成 `TaskConfig` 后传递给引擎：

```
┌─────────────┐     Feign      ┌─────────────┐     TaskConfig     ┌──────────────┐
│ Integration  │  获取解密凭据   │  Metadata    │   完整配置传递    │   Engine      │
│ (调用方)     │ ────────────→ │  (数据源)    │ ───────────────→ │  (执行层)     │
└─────────────┘                └─────────────┘                   └──────────────┘
```

## API 一览

### Job 查询 `/api/v1/jobs`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{jobId}?engineType=CANAL` | 查询引擎 Job 状态与指标 |

## 配置

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}    # Canal 引擎 Kafka 地址
    consumer:
      group-id: relake-canal-consumer
      auto-offset-reset: earliest

flink:
  gateway:
    url: ${FLINK_GATEWAY_URL:http://localhost:8083}       # Flink SQL Gateway 地址
```

## 依赖关系

```
relake-executor
├── relake-common (统一响应体 R、ResultCode)
├── spring-boot-starter-web
├── spring-cloud-starter-alibaba-nacos-discovery
├── spring-cloud-starter-alibaba-nacos-config
├── spring-kafka (Canal Kafka 消费)
├── canal.client (Canal Java 客户端 1.1.7)
├── okhttp (Flink SQL Gateway REST 调用)
├── knife4j-openapi3-jakarta-spring-boot-starter
└── hutool-all (通过 relake-common 传递)
```
