# relake-integration

ReLake 数据集成核心服务 —— 引擎无关的任务编排、状态机管理与引擎路由。

## 定位

`relake-integration` 是 ReLake 的**任务编排层**，面向用户提供同步任务的 CRUD 和生命周期管理。它不执行实际的 CDC 采集工作，而是通过策略模式将任务路由到 `relake-executor` 中的具体引擎实现。同时，通过 OpenFeign 调用 `relake-metadata` 获取解密后的数据源/目标凭据，组装成 `TaskConfig` 传递给引擎。

## 文件结构

```
relake-integration/src/main/java/com/relake/integration/
├── IntegrationApplication.java       # 启动类（@EnableFeignClients, @MapperScan, 端口 8083）
├── entity/
│   └── Task.java                     # 同步任务实体 → ds_task 表
├── mapper/
│   └── TaskMapper.java               # MyBatis-Plus BaseMapper
├── model/
│   └── TaskStatus.java               # 任务状态枚举：DRAFT → VALIDATING → READY → RUNNING → FAILED / STOPPED
├── dto/
│   ├── TaskCreateRequest.java        # 创建请求体（name, datasourceId, targetId, engineType, sourceTables...）
│   ├── TaskUpdateRequest.java        # 更新请求体（字段可选）
│   ├── TaskVO.java                   # 响应体 + static from(Task)
│   ├── DatasourceDTO.java            # Feign 响应 VO（含解密 password）
│   └── TargetDTO.java                # Feign 响应 VO（含解密 secretKey）
├── feign/
│   └── MetadataClient.java           # OpenFeign 接口 → relake-metadata /internal/**
├── config/
│   ├── FeignConfig.java              # Feign 请求拦截器（注入 X-Internal-Call 头）+ 重试策略
│   └── MyMetaObjectHandler.java      # createTime / updateTime 自动填充
├── orchestration/
│   ├── TaskStateMachine.java         # 【核心】状态机：定义并校验合法状态转换
│   └── TaskOrchestrator.java         # 【核心】任务编排器：校验 → 路由引擎 → 提交 → 跟踪
├── service/
│   ├── TaskService.java              # 接口：CRUD + validate + start + stop + status + metrics
│   └── impl/
│       └── TaskServiceImpl.java      # 实现（委托编排器 + Mapper）
└── controller/
    └── TaskController.java           # REST: /api/v1/tasks/**
```

## 核心设计

### 1. 服务间通信架构

```
前端 / Postman
      │ POST /api/v1/tasks
      ▼
┌─────────────────┐
│  Gateway (8080)  │  JWT 鉴权 → 转发 X-User-Id / X-Username
└────────┬────────┘
         │
         ▼
┌──────────────────────┐           OpenFeign           ┌──────────────────┐
│  Integration (8083)  │ ────────────────────────────→ │ Metadata (8082)  │
│                      │   GET /internal/datasources/   │                  │
│  TaskController      │   GET /internal/targets/       │ 返回解密凭据     │
│    → TaskService     │                                │                  │
│      → Orchestrator  │  本地 Spring Bean 调用          │                  │
│        → Engine      │ ────────────────────────────→ │  Executor (8084) │
│                      │   factory.getEngine(type)      │  SyncEngine 执行  │
└──────────────────────┘                                └──────────────────┘
```

### 2. 任务状态机

```
DRAFT —→ VALIDATING —→ READY —→ RUNNING —→ STOPPED
  │         │            │         │
  │         │            │         │
  └─────────┴────────────┴──→ FAILED ←──┘
                              ↑ (可重试回 VALIDATING)
```

| 状态 | 含义 | 可迁入 |
|------|------|--------|
| **DRAFT** | 新建未验证，刚创建 | VALIDATING, FAILED |
| **VALIDATING** | 正通过 Feign 校验数据源/目标是否存在 | READY, FAILED |
| **READY** | 校验通过，可启动 | RUNNING, FAILED |
| **RUNNING** | 引擎正在执行 CDC 采集 | FAILED, STOPPED |
| **FAILED** | 执行失败，记录 errorMessage | VALIDATING（重试） |
| **STOPPED** | 手动停止 | —（终态） |

`TaskStateMachine` 硬编码所有合法转换，非法转换抛出 `IllegalStateException`。

### 3. 任务编排流程

```
create → validate → start → (running) → stop
  │         │         │
  │         │         │
  ▼         ▼         ▼
DRAFT    READY     RUNNING          STOPPED
```

**validate 流程：**
```
1. 状态机: DRAFT → VALIDATING
2. Feign 调用 metadata: GET /internal/datasources/{id}
3. Feign 调用 metadata: GET /internal/targets/{id}
4. factory.getEngine(type) 获取引擎
5. engine.validate(taskConfig) 引擎校验
6. 状态机: VALIDATING → READY
```

**start 流程：**
```
1. 状态机: READY → RUNNING
2. Feign 获取解密凭据（同 validate）
3. 组装 TaskConfig（含解密 password/secretKey）
4. engine.submit(taskConfig) → JobHandle
5. 序列化 JobHandle 到 task.jobHandleJson
6. 状态机: READY → RUNNING
```

**stop 流程：**
```
1. 状态机: RUNNING → STOPPED
2. 反序列化 JobHandle
3. engine.stop(handle)
```

### 4. 引擎路由

`TaskOrchestrator` 通过 `EngineType.valueOf(task.getEngineType())` 将数据库中的字符串转为枚举，再由 `SyncEngineFactory.getEngine(type)` 获取对应引擎 Bean：

```
task.engineType = "CANAL"      → CanalEngine
task.engineType = "FLINK_CDC"  → FlinkCdcEngine
task.engineType = "DATAX"      → DataXEngine
```

### 5. 服务间安全

Integration 调用 Metadata 的内部端点 `/internal/**`，通过 `FeignConfig` 注入 `X-Internal-Call: true` 请求头。该路径不在 Gateway 路由白名单中，外部请求无法直接访问，仅服务间 Feign 调用可达。

## API 一览

### 同步任务 `/api/v1/tasks`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建任务 → DRAFT |
| GET | `/` | 分页查询 `?page=1&size=10&keyword=xxx` |
| GET | `/list` | 全部任务 |
| GET | `/{id}` | 查询详情 |
| PUT | `/{id}` | 更新任务 |
| DELETE | `/{id}` | 删除任务 |
| POST | `/{id}/validate` | 校验配置 → VALIDATING → READY |
| POST | `/{id}/start` | 启动任务 → RUNNING |
| POST | `/{id}/stop` | 停止任务 → STOPPED |
| GET | `/{id}/status` | 查询引擎 Job 实时状态 |
| GET | `/{id}/metrics` | 查询运行指标 |

### 请求示例

**创建任务：**
```json
POST /api/v1/tasks
{
    "name": "业务库-用户同步",
    "datasourceId": 1,
    "targetId": 1,
    "engineType": "CANAL",
    "sourceTables": "users,orders",
    "configJson": "{\"parallelism\": 1}",
    "description": "同步业务库的用户和订单表"
}
```

**启动任务：**
```
POST /api/v1/tasks/1/start
```

**运行状态：**
```json
GET /api/v1/tasks/1/status
→ { "code": 0, "data": "RUNNING" }
```

## ds_task 表结构

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花算法） |
| name | VARCHAR(100) | 任务名称（唯一） |
| datasource_id | BIGINT | 源数据源 ID |
| target_id | BIGINT | 目标存储 ID |
| engine_type | VARCHAR(20) | CANAL / FLINK_CDC / DATAX |
| source_tables | TEXT | 源表列表（逗号分隔） |
| status | VARCHAR(20) | 状态：DRAFT / VALIDATING / READY / RUNNING / FAILED / STOPPED |
| config_json | TEXT | 引擎专属配置（JSON） |
| cron_expr | VARCHAR(50) | DataX 定时表达式 |
| job_handle_json | TEXT | JobHandle 序列化 |
| error_message | TEXT | 最近错误信息 |
| description | VARCHAR(500) | 描述 |

## 依赖关系

```
relake-integration
├── relake-common (R、ResultCode、BusinessException、GlobalExceptionHandler)
├── relake-executor (SyncEngine、SyncEngineFactory、模型类)
├── spring-boot-starter-web
├── spring-cloud-starter-alibaba-nacos-discovery
├── spring-cloud-starter-alibaba-nacos-config
├── spring-cloud-starter-openfeign (→ relake-metadata)
├── spring-cloud-starter-loadbalancer
├── mybatis-plus-spring-boot3-starter (ds_task CRUD)
├── mysql-connector-j (metadata DB 连接)
└── knife4j-openapi3-jakarta-spring-boot-starter
```
