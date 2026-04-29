# ReLake — 实时数据湖项目技术方案与实施计划

## 一、背景与目标

构建一个实时数据湖平台（ReLake），具备以下核心能力：
- 基于CDC（Change Data Capture）的实时数据库采集同步
- 数据落入湖仓一体存储，支持后续OLAP分析
- 微服务架构，前后端分离，中间件容器化部署

当前阶段聚焦**数据集成模块**，打通「源数据库 → Kafka → 数据湖」全链路。

---

## 二、技术选型

### 2.1 后端微服务

| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 基础框架 | Spring Boot + Spring Cloud | 3.3.x / 2023.0.x | 主流微服务框架 |
| 注册/配置中心 | Nacos | 2.3.x | 阿里开源，服务发现+配置管理一体 |
| API网关 | Spring Cloud Gateway | 3.x | 与Spring生态无缝集成 |
| ORM | MyBatis-Plus | 3.5.x | 代码生成器、分页插件、零SQL开销 |
| 内部认证 | JWT（网关层验证） | jjwt 0.12 | 无状态，网关统一鉴权 |

### 2.2 CDC数据集成（多引擎架构）

集成模块设计为**引擎无关**的抽象层，通过策略模式支持多种采集引擎：

```
                    ┌──────────────────────────────┐
                    │      Integration Service      │
                    │  (Task Orchestrator — 引擎无关) │
                    └──────────┬───────────────────┘
                               │ 策略模式
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │ Canal引擎    │  │Flink CDC引擎│  │  DataX引擎   │
    │ (实时CDC)    │  │ (实时CDC)    │  │ (批量同步)   │
    │ MySQL binlog │  │全量+增量一体 │  │ 一次性/定时  │
    └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
           │                │                │
           ▼                ▼                ▼
       Kafka Topic    Flink Job       DataX Job
```

| 组件 | 选型 | 适用场景 |
|------|------|----------|
| Canal | MySQL binlog实时解析 | MySQL CDC实时增量采集 |
| Flink CDC 3.x | 全量+增量一体化 | MySQL/PostgreSQL实时CDC，需要exactly-once语义 |
| DataX | 离线批量同步 | 全量初始化、定时批量迁移、异构数据源同步 |
| 消息队列 | Apache Kafka 3.7（KRaft模式） | Canal输出通道 + Flink CDC中间缓冲 |

> **设计要点**：每种引擎实现统一接口 `SyncEngine`，各自负责启动/停止/监控。任务配置中通过 `engineType` 字段指定使用哪种引擎，新增引擎只需实现接口即可。

### 2.3 数据湖存储

| 组件 | 选型 | 说明 |
|------|------|------|
| 数据湖格式 | **Apache Paimon** | Flink原生，LSM存储引擎读写效率高，内置Schema演化，支持CDC入湖后自动生成可查询快照 |
| 对象存储 | MinIO | 自建S3兼容存储，容器化部署 |
| OLAP查询（后续） | StarRocks | MPP引擎，支持Paimon Catalog直接查询 |

### 2.4 前端

| 组件 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3.4+ | Composition API + `<script setup>` |
| UI组件库 | Element Plus | 国内最主流企业级UI库 |
| 开箱模板 | Vben Admin (v5) | 基于Vue3+Vite+TypeScript的开箱管理后台，内置权限/主题/国际化 |
| 构建工具 | Vite | 快速HMR，Vue官方推荐 |
| 状态管理 | Pinia | Vue官方推荐，类型安全 |

### 2.5 基础设施（Docker Compose）

```
┌─────────────────────────────────────────────────┐
│  Nacos 2.3   │  Kafka 3.7   │  MinIO   │ Flink  │
│  (8848)      │  (9092)      │  (9000)  │(8081)  │
├──────────────┴──────────────┴──────────┴────────┤
│  MySQL(source) │ MySQL(metadata) │ StarRocks     │
│  (3307)        │ (3306)          │ (9030)        │
└──────────────────────────────────────────────────┘
```

---

## 三、微服务模块设计

```
relake/
├── pom.xml                          # 根POM（依赖管理）
├── docker-compose.yml               # 基础设施容器编排
├── docker/                          # Flink CDC connector JAR目录
├── relake-common/                   # 公共模块（DTO、异常、工具类）
├── relake-gateway/                  # API网关（Spring Cloud Gateway + JWT）
├── relake-metadata/                 # 元数据管理服务
│   ├── 数据源连接管理 (CRUD)
│   ├── 目标存储配置 (CRUD)
│   ├── Schema发现与管理
│   └── 任务配置持久化
├── relake-integration/              # 数据集成服务（核心）
│   ├── 引擎无关的同步任务管理（策略模式）
│   ├── SyncEngine接口 + Canal/FlinkCDC/DataX实现
│   ├── 任务状态机管理（DRAFT → RUNNING → FAILED/STOPPED）
│   └── 引擎选择与调度
├── relake-executor/                 # 任务执行器
│   ├── CanalClient（binlog订阅、位点管理）
│   ├── FlinkJobSubmitter（Flink SQL Gateway REST封装）
│   ├── DataXExecutor（DataX JSON配置生成与运行）
│   └── 统一的JobRunner调度器
└── relake-web/                      # 前端项目（Vue3 + Vben Admin + Element Plus）
```

**服务通信架构：**
```
前端 → Gateway(8080) → Metadata(8082)
                     → Integration(8083)
                     → Executor(8084)

Integration → Executor（引擎路由）
   ├────→ CanalClient → Kafka → Paimon Ingestion
   ├────→ FlinkJobSubmitter → Flink SQL Gateway → Flink Job → Paimon Sink → MinIO
   └────→ DataXExecutor → DataX JSON Job → Paimon/MinIO

Integration → Metadata（获取/更新任务配置及Schema）
```

---

## 四、数据集成模块核心设计

### 4.1 任务状态机

```
CREATE ──→ CONFIG_VALIDATING ──→ SNAPSHOT_INIT ──→ INCREMENTAL
               │                      │
               ▼                      ▼
          CONFIG_INVALID          FAILED（可重试）

STOPPED ←── 任意状态（主动停止）
```

### 4.2 CDC全链路数据流

**Canal路径（轻量实时）：**
```
源MySQL → Canal Server → Kafka Topic → CanalClient(自研) → Paimon表 → MinIO
```

**Flink CDC路径（生产级实时）：**
```
源MySQL/PostgreSQL → Flink CDC Connector → Flink Paimon Sink → MinIO(Paimon表)
                                                       ↘ StarRocks外表（后续）
```

**DataX路径（批量同步）：**
```
源DB → DataX Reader → DataX Writer → 目标（可用于全量初始化Paimon表）
```

### 4.3 引擎策略抽象

```java
// relake-executor中的核心接口
public interface SyncEngine {
    EngineType getType();                       // CANAL / FLINK_CDC / DATAX
    boolean validate(TaskConfig config);         // 校验配置
    JobHandle submit(TaskConfig config);         // 提交任务，返回Job句柄
    void stop(JobHandle handle);                 // 停止任务
    JobStatus getStatus(JobHandle handle);       // 查询状态
    Metrics getMetrics(JobHandle handle);        // 运行指标
}
```

每种引擎各自实现该接口，`Integration`层通过工厂模式根据任务类型路由到对应引擎，新增引擎只需实现接口并注册即可。

### 4.4 关键API设计（Integration服务）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/tasks` | POST | 创建同步任务 |
| `/api/v1/tasks/{id}` | GET | 查询任务详情 |
| `/api/v1/tasks/{id}/start` | POST | 启动任务（路由到对应引擎执行） |
| `/api/v1/tasks/{id}/stop` | POST | 停止任务 |
| `/api/v1/tasks/{id}/metrics` | GET | 获取任务运行指标 |
| `/api/v1/datasources` | CRUD | 管理数据源连接 |
| `/api/v1/datasources/{id}/test` | POST | 测试连接 |
| `/api/v1/targets` | CRUD | 管理目标存储 |

---

## 五、分阶段实施计划

### Phase 1：基础设施搭建
- [ ] 编写 `docker-compose.yml`：Nacos、Kafka(KRaft)、MinIO、MySQL×2、Canal Server
- [ ] Flink Session Cluster（可选，Flink CDC引擎需要时启用）
- [ ] 验证所有容器正常启动、网络互通

### Phase 2：项目脚手架 + 公共模块
- [ ] 创建Maven多模块项目 `pom.xml`
- [ ] `relake-common`：统一响应体 `R<T>`、全局异常处理 `GlobalExceptionHandler`、基础工具类
- [ ] Nacos共享配置注册（数据库连接、公共参数）

### Phase 3：网关服务
- [ ] Spring Cloud Gateway 路由配置
- [ ] JWT令牌生成与网关层验证
- [ ] 跨域配置

### Phase 4：元数据管理服务
- [ ] 数据源连接管理（MySQL/PostgreSQL CRUD + 连接测试）
- [ ] 目标存储配置管理（MinIO + Paimon配置）
- [ ] Schema发现：连接源库获取表结构
- [ ] REST API + MyBatis-Plus + MySQL存储

### Phase 5：集成服务 + 执行器（多引擎）
- [ ] `relake-executor`：`SyncEngine`接口定义 + CanalClient实现
- [ ] `relake-executor`：FlinkJobSubmitter（Flink SQL Gateway REST封装）
- [ ] `relake-executor`：DataXExecutor（DataX任务配置生成与执行）
- [ ] `relake-integration`：统一任务管理（CRUD + 引擎路由 + 状态机）
- [ ] 任务启动/停止/监控（支持多引擎切换）

### Phase 6：前端搭建
- [ ] Vue3 + Vben Admin (v5) 项目初始化
- [ ] 数据源管理页面（增删改查 + 连接测试）
- [ ] 任务管理页面（创建/启动/停止/监控，含引擎选择）

### Phase 7：端到端联调
- [ ] Canal路径：MySQL CDC → Canal → Kafka → CanalClient → Paimon表 → MinIO
- [ ] Flink CDC路径：MySQL → Flink CDC → Paimon Sink → MinIO
- [ ] DataX路径：MySQL全量同步 → Paimon表
- [ ] 前端发起不同引擎的同步任务，观察数据变化
- [ ] 增量同步验证（源表INSERT/UPDATE/DELETE）

---

## 六、验证方式

1. **Phase 1**: `docker ps` 确认所有容器健康；`curl localhost:8848/nacos` Nacos可访问
2. **Phase 2**: `mvn clean install` 编译通过
3. **Phase 3**: Postman/curl 测试网关路由转发
4. **Phase 4**: REST API测试数据源CRUD + 连接测试
5. **Phase 5**: 在Flink Web UI(8081)能看到提交的Job
6. **Phase 6**: 前端页面正常渲染，可进行数据源管理操作
7. **Phase 7**: 源MySQL插入数据，MinIO中出现对应的Paimon表文件；三种引擎均通过验证

---

## 七、关键文件清单

| 文件 | 说明 |
|------|------|
| `pom.xml` | 根Maven反应堆POM，定义所有模块、依赖版本 |
| `docker-compose.yml` | 所有基础设施容器编排 |
| `relake-common/.../R.java` | 统一响应体（所有服务共用） |
| `relake-common/.../GlobalExceptionHandler.java` | 全局异常处理 |
| `relake-gateway/.../GatewayApplication.java` | 网关启动类+路由配置 |
| `relake-integration/.../SyncEngine.java` | 引擎抽象接口（策略模式核心） |
| `relake-integration/.../TaskOrchestrator.java` | 任务编排核心逻辑（引擎路由） |
| `relake-executor/.../CanalClient.java` | Canal引擎实现（binlog订阅消费） |
| `relake-executor/.../FlinkJobSubmitter.java` | Flink CDC引擎实现（SQL Gateway REST客户端） |
| `relake-executor/.../DataXExecutor.java` | DataX引擎实现（JSON配置生成与执行） |
| `relake-web/` | Vue3 + Vben Admin 前端项目 |
