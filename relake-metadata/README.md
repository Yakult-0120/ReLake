# relake-metadata

ReLake 元数据管理服务 —— 数据源 / 目标存储的配置管理与 Schema 发现。

## 文件结构

```
relake-metadata/src/main/java/com/relake/metadata/
├── MetadataApplication.java          # 启动类（@MapperScan 扫包）
├── entity/
│   ├── Datasource.java               # 数据源实体 → ds_datasource 表
│   └── Target.java                   # 目标存储实体 → ds_target 表
├── mapper/
│   ├── DatasourceMapper.java         # MyBatis-Plus BaseMapper
│   └── TargetMapper.java             # MyBatis-Plus BaseMapper
├── service/
│   ├── DatasourceService.java        # 接口：CRUD + testConnection + getEntity
│   ├── TargetService.java            # 接口：CRUD + testConnection
│   └── impl/
│       ├── DatasourceServiceImpl.java  # JDBC 连接测试 + AES 密码加密
│       └── TargetServiceImpl.java      # MinIO HTTP 健康检查 + AES 加密
├── controller/
│   ├── DatasourceController.java     # REST: /api/v1/datasources/**
│   ├── TargetController.java         # REST: /api/v1/targets/**
│   └── SchemaController.java         # REST: /api/v1/schemas/**
├── dto/
│   ├── DatasourceRequest.java        # 创建/更新请求体
│   ├── DatasourceVO.java             # 响应体（不含密码）
│   ├── TargetRequest.java
│   ├── TargetVO.java                 # 响应体（不含 SecretKey）
│   └── TableInfo.java                # Schema 发现结果
├── config/
│   └── MyMetaObjectHandler.java      # createTime / updateTime 自动填充
└── util/
    └── AesUtil.java                  # 密码 AES 加解密
```

## 核心设计

### 1. 数据源管理

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Controller  │ ──→ │  DatasourceService │ ──→ │  DatasourceMapper │
│  (REST)      │ ←── │  (业务逻辑)        │ ←── │  (MyBatis-Plus)   │
└─────────────┘     └──────────────────┘     └─────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
        AesUtil         JDBC Driver    ServiceImpl
       (密码加解密)     (连接测试)      (CRUD 基类)
```

**数据源实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 数据源唯一名称 |
| dbType | String | MYSQL / POSTGRESQL |
| host | String | 主机地址 |
| port | Integer | 端口 |
| dbName | String | 数据库名 |
| username | String | 连接用户 |
| password | String | **AES 加密存储** |
| extraParams | String | 额外 JDBC 连接参数 |
| status | String | ACTIVE / DISABLED |

**连接测试流程：**
```
getEntity(id) → 获取含加密密码的完整实体
    │
    ├── aesUtil.decrypt(password) → 解密
    ├── buildJdbcUrl() → jdbc:mysql://host:port/db?params
    └── DriverManager.getConnection() → conn.isValid(5)
```

### 2. 目标存储管理

```
┌─────────────┐     ┌───────────────┐     ┌──────────────┐
│  Controller  │ ──→ │  TargetService │ ──→ │  TargetMapper │
└─────────────┘     └───────────────┘     └──────────────┘
                            │
                     ┌──────┴──────┐
                     ▼             ▼
                AesUtil        HttpUtil
               (加解密)      (MinIO 健康检查)
```

**连接测试：** 向 MinIO 的 `/minio/health/live` 发起 GET 请求，5 秒超时。

### 3. Schema 发现

不预设任何元数据缓存，每次直接从源库实时查询 `java.sql.DatabaseMetaData`：

```
GET /api/v1/schemas/{datasourceId}/tables
  └── 返回 List<String> 表名列表

GET /api/v1/schemas/{datasourceId}/tables/{tableName}
  └── 返回 TableInfo { 表名, 注释, 列信息[] }
      └── ColumnInfo { 列名, 类型, JDBC Type Code, 是否可空, 是否主键, 注释 }
```

### 4. 密码安全

```
写入：明文 → AesUtil.encrypt() → Base64 密文 → DB
读取：DB → Base64 密文 → AesUtil.decrypt() → 明文
返回前端：DatasourceVO（不含 password 字段）
```

## API 一览

### 数据源 `/api/v1/datasources`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建数据源 |
| GET | `/` | 分页查询 `?page=1&size=10&keyword=xxx` |
| GET | `/list` | 全部可用数据源（status=ACTIVE） |
| GET | `/{id}` | 查询详情 |
| PUT | `/{id}` | 更新（密码不传则不修改） |
| DELETE | `/{id}` | 删除 |
| POST | `/{id}/test` | **测试连接** |

### 目标存储 `/api/v1/targets`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建目标存储 |
| GET | `/` | 分页查询 |
| GET | `/list` | 全部可用目标 |
| GET | `/{id}` | 查询详情 |
| PUT | `/{id}` | 更新 |
| DELETE | `/{id}` | 删除 |
| POST | `/{id}/test` | **测试 MinIO 连通性** |

### Schema `/api/v1/schemas`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/{datasourceId}/tables` | 获取源库所有表名 |
| GET | `/{datasourceId}/tables/{name}` | 获取指定表的列详情 |

## 依赖关系

```
relake-metadata
├── relake-common (统一响应体 R、BaseEntity、全局异常处理)
├── spring-boot-starter-web
├── spring-cloud-starter-alibaba-nacos-discovery
├── spring-cloud-starter-alibaba-nacos-config
├── mybatis-plus-spring-boot3-starter
├── mysql-connector-j
├── hutool-all (BeanUtil 复制、HttpUtil 请求、SecureUtil 加密)
└── knife4j-openapi3-jakarta-spring-boot-starter (API 文档)
```
