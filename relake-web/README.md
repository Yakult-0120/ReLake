# relake-web — ReLake 前端

Vue 3 + Vite + TypeScript + Element Plus 纯净搭建的管理后台。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5+ | Composition API + `<script setup>` |
| Vite | 8.x | 构建工具 + 开发服务器 |
| TypeScript | 5.x | 类型安全 |
| Element Plus | 2.x | UI 组件库（ElTable、ElForm、ElDialog、ElSelect 等） |
| Vue Router | 4.x | SPA 路由 + 导航守卫 |
| Pinia | 2.x | 状态管理（token 持久化） |
| Axios | 1.x | HTTP 请求 + JWT 拦截器 |

## 文件结构

```
relake-web/
├── index.html                  # HTML 入口
├── package.json                # 依赖与脚本
├── vite.config.ts              # Vite 配置（代理 /api → Gateway）
├── env.d.ts                    # TypeScript 环境声明
├── .env.development            # 开发环境变量
├── .env.production             # 生产环境变量
└── src/
    ├── main.ts                 # 应用入口（Element Plus + 图标全局注册）
    ├── App.vue                 # 根组件（纯 RouterView 出口）
    ├── router/
    │   └── index.ts            # 6 路由 + 导航守卫 (beforeEach)
    ├── stores/
    │   ├── auth.ts             # 认证状态（token + username localStorage）
    │   └── app.ts              # 全局状态（侧边栏折叠）
    ├── api/
    │   ├── request.ts          # Axios 实例 + JWT 拦截器 + 401 处理
    │   ├── auth.ts             # 登录 API
    │   ├── datasource.ts       # 数据源 CRUD + 测试连接
    │   ├── target.ts           # 目标存储 CRUD + 测试连接
    │   ├── schema.ts           # Schema 发现
    │   └── task.ts             # 任务 CRUD + 生命周期（start/stop/status/metrics）
    ├── layout/
    │   └── MainLayout.vue      # 主布局（侧边栏 + 顶栏 + 内容区）
    ├── views/
    │   ├── login/
    │   │   └── LoginView.vue           # 登录页
    │   ├── dashboard/
    │   │   └── DashboardView.vue       # 仪表盘（统计卡片 + 引擎介绍）
    │   ├── datasource/
    │   │   └── DatasourceListView.vue  # 数据源管理（CRUD + 连接测试弹窗）
    │   ├── target/
    │   │   └── TargetListView.vue      # 目标存储管理（CRUD + 连接测试弹窗）
    │   ├── schema/
    │   │   └── SchemaView.vue          # Schema 浏览器（数据源 → 表列表 → 列展开）
    │   └── task/
    │       └── TaskListView.vue        # 任务管理（CRUD + 状态机按钮 + 指标弹窗）
    └── styles/
        └── global.css          # 全局样式
```

## 页面说明

| 路由 | 页面 | 功能 |
|------|------|------|
| `/login` | LoginView | 用户名密码登录，获得 JWT 后跳转仪表盘 |
| `/dashboard` | DashboardView | 数据源/目标/任务数量统计卡片，三引擎介绍 |
| `/datasources` | DatasourceListView | 数据源 CRUD，支持 MYSQL/POSTGRESQL，含 JDBC 连接测试 |
| `/targets` | TargetListView | 目标存储 CRUD，支持 MINIO/PAIMON，含 HTTP 健康检查 |
| `/schemas` | SchemaView | 选择数据源 → 浏览表列表 → 点击展开列详情（列名/类型/可空/主键/注释） |
| `/tasks` | TaskListView | 同步任务 CRUD，引擎选择（CANAL/FLINK_CDC/DATAX），根据状态动态显示操作按钮 |

## 任务状态机 — 操作按钮动态显示

| 状态 | 可用操作 |
|------|----------|
| DRAFT | 编辑、校验、删除 |
| VALIDATING | — （过渡态） |
| READY | 启动、编辑、删除 |
| RUNNING | 停止、查看状态、查看指标 |
| FAILED | 重试(校验)、编辑、删除 |
| STOPPED | 查看状态、删除 |

## 核心设计

### JWT 认证流程

```
LoginView → POST /api/v1/auth/login → Gateway AuthController
    ↓ 返回 token
Pinia authStore → localStorage 持久化
    ↓
Axios request interceptor → 每次请求注入 Authorization: Bearer <token>
    ↓
Axios response interceptor → 401 → 清除 token → 跳转 /login
```

### Vite 开发代理

```ts
// vite.config.ts
server: {
  proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } }
}
```

开发模式下所有 `/api/*` 请求代理到 Gateway(8080)，JWT 鉴权由 Gateway 统一处理。生产构建时通过 `.env.production` 的 `VITE_API_BASE_URL` 指定后端地址。

### 导航守卫

```
router.beforeEach:
  /login     → 已登录 → 跳转 /dashboard
  其他路径   → 未登录 → 跳转 /login
```

## 快速开始

### 前置条件

- Node.js 20+
- ReLake 后端服务已启动（Gateway 8080）

### 安装与启动

```bash
cd relake-web
npm install
npm run dev
```

访问 http://localhost:5173 ，使用 admin/admin 登录。

### 构建生产版本

```bash
npm run build
# 产物在 dist/ 目录
```

## 与后端对应关系

| 前端 API 模块 | 后端微服务 | 端口 |
|--------------|-----------|------|
| `api/auth.ts` | Gateway (AuthController) | 8080 |
| `api/datasource.ts` | Metadata (DatasourceController) | 8082 |
| `api/target.ts` | Metadata (TargetController) | 8082 |
| `api/schema.ts` | Metadata (SchemaController) | 8082 |
| `api/task.ts` | Integration (TaskController) | 8083 |
