# ReLake — 实时数据湖平台

## 项目简介

ReLake 是一个实时数据湖平台，支持多引擎 CDC 数据采集（Canal / Flink CDC / DataX），
将源数据库的变更数据实时同步到 Apache Paimon 湖仓一体存储中。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端框架 | Spring Boot 3.3 + Spring Cloud 2023 + Spring Cloud Alibaba 2023 |
| 注册/配置中心 | Nacos 2.3 |
| API 网关 | Spring Cloud Gateway + JWT |
| ORM | MyBatis-Plus 3.5 |
| CDC 引擎 | Canal 1.1.7 / Flink CDC 3.x / DataX |
| 消息队列 | Apache Kafka 3.7 (KRaft) |
| 对象存储 | MinIO |
| 数据湖格式 | Apache Paimon |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |

## 模块结构

```
relake/
├── relake-common/        公共模块 (R<T>, 异常处理, BaseEntity)
├── relake-gateway/       API 网关 (8080) — JWT 鉴权 + 路由分发
├── relake-metadata/      元数据管理 (8082) — 数据源/目标 CRUD + Schema 发现
├── relake-integration/   集成服务 (8083) — 引擎无关任务编排 + 状态机
├── relake-executor/      执行器 (8084) — 多引擎实现 (Canal/FlinkCDC/DataX)
├── relake-web/           前端 — Vue 3 + Element Plus
├── docker-compose.yml    基础设施编排
└── docker/               SQL 初始化 + 配置
```

## 快速启动

### 前置条件

- JDK 17+
- Maven 3.9+
- Node.js 20+ (含 npm/pnpm)
- Docker & Docker Compose

### 1. 启动基础设施

```bash
# 启动所有 Docker 容器 (Nacos, Kafka, MinIO, MySQL×2, Canal Server)
docker-compose up -d

# 等待所有容器 Healthy (约 60s)
docker ps
```

### 2. 预加载 Nacos 配置

```bash
# 发布共享配置（所有后端服务引用的 relake-common.yaml）
curl -X POST "http://localhost:8848/nacos/v1/cs/configs" \
  -d "dataId=relake-common.yaml&group=DEFAULT_GROUP&content=spring: {}"
```

### 3. 构建项目

```bash
# 编译全部模块
mvn clean install -DskipTests
```

### 4. 启动微服务（按顺序，各开一个终端）

```bash
# 终端 1 — 元数据服务
mvn -pl relake-metadata spring-boot:run

# 终端 2 — 执行器
mvn -pl relake-executor spring-boot:run

# 终端 3 — 集成服务
mvn -pl relake-integration spring-boot:run

# 终端 4 — 网关
mvn -pl relake-gateway spring-boot:run
```

### 5. 启动前端

```bash
cd relake-web
npm install
npm run dev
# 访问 http://localhost:5173
```

### 6. 验证

见下方「端到端验证」章节。

## 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Nacos | 8848 | 注册中心 + 配置中心 |
| Kafka | 9092 | 消息队列 |
| MinIO API | 9000 | 对象存储 |
| MinIO Console | 9001 | Web 管理界面 |
| Metadata MySQL | 3306 | 元数据库 |
| Source MySQL | 3307 | 业务库 (CDC 源) |
| Canal Server | 11111 | CDC 采集 |
| Gateway | 8080 | API 入口 |
| Metadata | 8082 | 元数据服务 |
| Integration | 8083 | 集成服务 |
| Executor | 8084 | 执行器 |
| Frontend Dev | 5173 | 前端开发服务器 |

## 端到端验证

完整的 CDC 全链路验证流程：

### 1. 登录

浏览器打开 http://localhost:5173 ，使用 `admin` / `admin` 登录。

### 2. 创建数据源

在「数据源管理」页面新建：
- 名称: `business-db`
- 类型: `MYSQL`
- 主机: `localhost`
- 端口: `3307`
- 数据库名: `business_db`
- 用户名: `relake`
- 密码: `relake123`
- 点击「测试连接」验证连通性

### 3. 创建目标存储

在「目标存储管理」页面新建：
- 名称: `relake-minio`
- 类型: `MINIO`
- 终端: `http://localhost:9000`
- Bucket: `relake`
- AccessKey: `admin`
- SecretKey: `admin123456`
- 区域: `us-east-1`
- 点击「测试连接」验证连通性

### 4. 创建同步任务

在「同步任务管理」页面新建：
- 任务名称: `business-cdc`
- 数据源: `business-db`
- 目标存储: `relake-minio`
- 引擎类型: `CANAL`
- 源表: `users,orders,products`
- 创建后点击「校验」→ 状态变为 `READY`
- 点击「启动」→ 状态变为 `RUNNING`

### 5. 验证 CDC 数据流

```bash
# 在源 MySQL 插入测试数据
docker exec relake-source-mysql mysql -u root -proot123 business_db \
  -e "INSERT INTO users (username, email) VALUES ('e2e-test', 'e2e@test.com');"

# 更新数据触发 UPDATE 事件
docker exec relake-source-mysql mysql -u root -proot123 business_db \
  -e "UPDATE users SET email = 'updated@test.com' WHERE username = 'e2e-test';"

# 删除数据触发 DELETE 事件
docker exec relake-source-mysql mysql -u root -proot123 business_db \
  -e "DELETE FROM users WHERE username = 'e2e-test';"
```

观察 executor 日志输出：
```
[Canal CDC] INSERT business_db.users | id=5, username=e2e-test, email=e2e@test.com
[Canal CDC] UPDATE business_db.users | id=5, username=e2e-test, email=updated@test.com
[Canal CDC] DELETE business_db.users | id=5, username=e2e-test
```

### 6. 查看运行指标

前端任务列表 → 点击「查看指标」→ 可见 recordsIn/recordsOut 非零。

### 7. 停止任务

点击「停止」→ 状态变为 `STOPPED`。

## 引擎对比

| 特性 | Canal | Flink CDC | DataX |
|------|-------|-----------|-------|
| 同步模式 | 实时增量 | 全量+增量 | 离线批量 |
| 源端要求 | MySQL binlog | MySQL/PG CDC | 任意 JDBC |
| 延迟 | 毫秒级 | 毫秒级 | 分钟级 |
| Exactly-Once | 否 | 是 | 否 |
| 适用场景 | 轻量CDC | 生产级CDC | 全量迁移/初始化 |
| Phase 7 状态 | **完整可用** (Kafka消费) | 骨架 + REST 客户端 | JSON生成 + ProcessBuilder |

## 许可证

MIT
