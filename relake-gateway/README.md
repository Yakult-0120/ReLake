# relake-gateway

ReLake API 网关 —— 统一入口、JWT 鉴权、路由分发。

## 文件结构

```
relake-gateway/src/main/java/com/relake/gateway/
├── GatewayApplication.java          # 启动类
├── config/
│   ├── JwtUtil.java                 # JWT 工具类（生成/校验/解析）
│   └── CorsConfig.java              # 跨域配置（基于 WebFlux CORS）
├── filter/
│   └── JwtAuthFilter.java           # 全局鉴权过滤器
├── controller/
│   ├── AuthController.java          # POST /api/v1/auth/login
│   └── HealthController.java        # GET  /api/v1/health
└── dto/
    ├── LoginRequest.java            # 登录请求体
    └── LoginResponse.java           # 登录响应体

relake-gateway/src/main/resources/
├── bootstrap.yml                    # Nacos 注册/配置中心地址
└── application.yml                  # 路由规则 + JWT 配置
```

## 核心设计

### 1. 整体架构

```
浏览器/前端
    │  http://localhost:8080
    ▼
┌─────────────────────────────────────────────────┐
│                  relake-gateway                   │
│                                                   │
│  ① JwtAuthFilter（全局）                          │
│     ├─ /api/v1/auth/login → 白名单放行            │
│     ├─ /api/v1/health     → 白名单放行            │
│     └─ 其他路径 → 校验 Bearer Token               │
│           ├─ 无效 → 401 JSON                      │
│           └─ 有效 → 将 userId/username 写入 Header │
│                                                   │
│  ② 路由分发（基于 Path + Nacos lb）               │
│     /api/v1/datasources/** → relake-metadata      │
│     /api/v1/tasks/**       → relake-integration   │
│     /api/v1/jobs/**        → relake-executor      │
│                                                   │
│  ③ AuthController（自身处理，不路由）             │
│     POST /api/v1/auth/login → 返回 JWT            │
└─────────────────────────────────────────────────┘
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
 metadata   integration  executor
 (8082)     (8083)       (8084)
```

### 2. 鉴权流程

```
请求进入 → JwtAuthFilter（GlobalFilter, Order=-100）
              │
              ├── 路径在白名单？ ──YES──→ 直接放行
              │
              └── NO
                    │
                    ├── Header 中有 Authorization: Bearer xxx？
                    │     └── NO  → 401 {"code":401,"message":"未认证或令牌已过期"}
                    │
                    └── YES
                          │
                          ├── JWT 签名/过期校验
                          │     └── 无效 → 401
                          │
                          └── 有效
                                将用户信息写入 Header：
                                ├── X-User-Id: 1
                                └── X-Username: admin
                                路由到下游服务
```

下游服务通过读取 `X-User-Id` / `X-Username` 请求头获取当前用户身份，无需再次校验 JWT。

### 3. 路由规则（application.yml）

| 路径前缀 | 路由目标 | 说明 |
|----------|----------|------|
| `/api/v1/auth/**` | 网关自身 | `AuthController` 直接处理，不转发 |
| `/api/v1/datasources/**` | `lb://relake-metadata` | 数据源管理 |
| `/api/v1/targets/**` | `lb://relake-metadata` | 目标存储管理 |
| `/api/v1/schemas/**` | `lb://relake-metadata` | Schema 管理 |
| `/api/v1/tasks/**` | `lb://relake-integration` | 采集任务管理 |
| `/api/v1/jobs/**` | `lb://relake-executor` | 作业执行管理 |

`lb://` 前缀表示通过 Nacos Service Discovery 做负载均衡路由，服务名不区分大小写。

### 4. JWT 配置

```yaml
relake:
  jwt:
    secret: <256-bit 密钥>     # 生产环境必须修改且使用环境变量
    expiration: 86400000       # Token 有效期（毫秒），默认 24 小时
```

### 5. 跨域

`CorsConfig` 允许任意来源、常用 HTTP 方法、任意 Header，支持 `credentials`。生产环境部署前应收紧 `allowedOriginPatterns`。

### 6. 登录接口（临时代码）

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin"
}

Response:
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

当前为内置 `admin/admin` 硬编码校验，后续 Phase 将对接独立的用户管理服务。

## 依赖关系

```
relake-gateway
├── relake-common (统一响应体 R、ResultCode)
├── spring-cloud-starter-gateway (路由引擎)
├── spring-cloud-starter-alibaba-nacos-discovery (服务发现)
├── spring-cloud-starter-alibaba-nacos-config (配置中心)
├── spring-cloud-starter-loadbalancer (负载均衡)
├── jjwt (JWT 令牌)
└── spring-boot-starter-validation (参数校验)
```
