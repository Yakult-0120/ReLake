# relake-job-agent

ReLake DataX 执行代理 —— XXL-JOB Executor，内嵌 DataX 运行环境。

## 定位

`relake-job-agent` 是 ReLake 的**DataX 执行层**，作为 XXL-JOB 2.4.0 的 Executor（执行器）运行。不直接暴露外部 API，只响应 XXL-JOB Admin 的调度请求。通过 `@XxlJob("dataxSync")` Handler 接收任务参数，从 Integration 内部 API 拉取 DataX JSON 配置后通过 ProcessBuilder 执行 Python 脚本。

## 文件结构

```
relake-job-agent/src/main/java/com/relake/job/agent/
├── JobAgentApplication.java          # 启动类（端口 8085）
├── handler/
│   └── DataXJobHandler.java          # @XxlJob("dataxSync") Handler
└── config/
    └── XxlJobConfig.java             # XxlJobSpringExecutor Bean 配置
```

## 核心设计

### 1. 调度流程

```
XXL-JOB Admin (8086)
      │ POST http://job-agent-host:9999/run
      ▼
┌─────────────────────────────────┐
│       Job Agent (8085)           │
│                                  │
│  XxlJobSpringExecutor            │
│    ↓                             │
│  @XxlJob("dataxSync")           │
│    ↓                             │
│  1. 解析 jobParam → taskId       │
│  2. GET /internal/tasks/{id}/datax-config  ← integration(8083)
│  3. 写入 DataX JSON 配置文件     │
│  4. ProcessBuilder 执行 DataX    │
│  5. stdout → XxlJobHelper.log()  │
│  6. exitCode 0 → handleSuccess   │
│     exitCode ≠ 0 → handleFail    │
│  7. 清理临时配置文件              │
└─────────────────────────────────┘
```

### 2. Handler 执行细节

```
XXL-JOB 触发携带参数 taskId
  │
  ▼
① GET http://integration:8083/internal/tasks/{taskId}/datax-config
  → DataXConfigDTO { command, args, workingDir, jobJson }
  │
  ▼
② Files.writeString(workingDir/datax-job-{taskId}.json, jobJson)
  │
  ▼
③ ProcessBuilder: [command, arg1, arg2, ...]
  - directory = workingDir
  - redirectErrorStream = true
  - 实时读取 stdout → XxlJobHelper.log()
  │
  ▼
④ process.waitFor()
  - exitCode == 0 → handleSuccess("DataX 同步完成")
  - exitCode != 0 → handleFail("DataX 执行失败, exitCode={}")
  │
  ▼
⑤ finally: Files.deleteIfExists(configFile)
```

### 3. 错误处理

所有异常通过 `XxlJobHelper.handleFail()` 上报，XXL-JOB Admin 记录为失败并可配置重试：

```java
try { ... } catch (Exception e) {
    XxlJobHelper.handleFail("DataX 执行异常: " + e.getMessage());
}
```

这确保失败信息对 XXL-JOB Admin 可见，handleCode 正确设置为 500。

## 配置

### XXL-JOB Executor 配置

```yaml
# application.yml
xxl:
  job:
    admin:
      addresses: http://xxl-job-admin:8086/xxl-job-admin  # Admin 地址
    executor:
      appname: relake-datax-executor      # 执行器名称（与 Admin 注册一致）
      port: 9999                           # Executor 通信端口
      logpath: /data/logs/xxl-job         # 本地日志路径
    accessToken: relake_token              # 通信令牌
```

### DataX 执行环境

```yaml
datax:
  home: /opt/datax                         # DataX Python 安装路径

relake:
  integration:
    url: http://host.docker.internal:8083   # Integration 内部 API 地址
```

## API（内部回调）

Handler 通过 REST 调用 Integration 的内部端点获取 DataX 配置：

```
GET http://integration:8083/internal/tasks/{taskId}/datax-config

Response: DataXConfigDTO
{
  "command": "/opt/datax/bin/datax.py",
  "args": "/opt/datax/job/datax-job-xxx.json",
  "workingDir": "/opt/datax",
  "jobJson": "{...完整的 DataX JSON 配置...}"
}
```

## 依赖关系

```
relake-job-agent
├── relake-common (DataXConfigDTO)
├── com.xuxueli:xxl-job-core:2.4.0
├── spring-boot-starter-web
├── spring-cloud-starter-alibaba-nacos-discovery
├── spring-cloud-starter-alibaba-nacos-config
└── lombok
```
