# relake-common

ReLake 公共模块 —— 所有微服务共享的基础组件，无业务逻辑，纯基础设施。

## 文件结构

```
relake-common/src/main/java/com/relake/common/
├── entity/
│   └── BaseEntity.java              # 实体基类
├── web/
│   ├── R.java                       # 统一响应体
│   ├── ResultCode.java              # 统一结果码枚举
│   ├── BusinessException.java       # 业务异常
│   └── GlobalExceptionHandler.java  # 全局异常处理器
└── constant/
    └── (预留，全局常量)
```

## 各组件设计

### 1. 统一响应体 `R<T>`

所有后端 API 的返回值格式统一为：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1714400000000
}
```

**用法示例：**
```java
// 成功
return R.ok(data);
return R.ok(data, "自定义提示");
return R.ok();  // 无数据

// 失败
return R.fail(ResultCode.BAD_REQUEST);
return R.fail(ResultCode.TASK_NOT_FOUND, "任务 123 不存在");
return R.fail(500, "自定义错误");

// 分页
return R.page(records, total, page, size);
```

### 2. 统一结果码 `ResultCode`

- **通用码（<1000）**：`SUCCESS`、`BAD_REQUEST`、`UNAUTHORIZED`、`FORBIDDEN`、`NOT_FOUND`、`INTERNAL_ERROR` 等
- **业务码（1000+）**：`DATASOURCE_CONNECT_FAILED`、`TASK_NOT_FOUND`、`ENGINE_NOT_SUPPORTED` 等，按模块分段扩展

新增业务码时在 `ResultCode.java` 中添加枚举值即可。

### 3. 实体基类 `BaseEntity`

所有数据库实体的父类，提供通用字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 主键，MyBatis-Plus 雪花算法自动生成 |
| `createTime` | LocalDateTime | 创建时间，自动填充 |
| `updateTime` | LocalDateTime | 更新时间，自动填充 |

实体继承 `BaseEntity` 后，无需手写 `id`、创建/更新时间字段。

### 4. 业务异常 `BusinessException`

```java
throw new BusinessException(ResultCode.TASK_NOT_FOUND, "任务 456 不存在");
```

被 `GlobalExceptionHandler` 捕获后，自动转换为 `R.fail()` 响应。

### 5. 全局异常处理 `GlobalExceptionHandler`

统一拦截：
- `BusinessException` → 返回对应的业务错误码
- `MethodArgumentNotValidException` → 参数校验失败（400）
- `BindException` → 参数绑定失败（400）
- `Exception` → 兜底返回 `INTERNAL_ERROR`（500）

每个业务服务启用自动扫包即可使用此处理器。

## 依赖

| 依赖 | 用途 |
|------|------|
| `lombok` | 减少样板代码 |
| `jakarta.servlet-api` | Servlet 标准（Web 层依赖） |
| `spring-boot-starter-web` | Spring MVC |
| `spring-boot-starter-validation` | 参数校验 |
| `mybatis-plus-spring-boot3-starter` | ORM 基础（BaseEntity 依赖） |
| `hutool-all` | 通用工具库 |
