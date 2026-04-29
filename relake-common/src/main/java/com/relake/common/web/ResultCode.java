package com.relake.common.web;

/**
 * 统一结果码
 */
public enum ResultCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务码 (1000+)
    DATASOURCE_CONNECT_FAILED(1001, "数据源连接失败"),
    DATASOURCE_ALREADY_EXISTS(1002, "数据源已存在"),
    TASK_NOT_FOUND(1003, "任务不存在"),
    TASK_START_FAILED(1004, "任务启动失败"),
    TASK_STOP_FAILED(1005, "任务停止失败"),
    TASK_CONFIG_INVALID(1006, "任务配置无效"),
    ENGINE_NOT_SUPPORTED(1007, "不支持的采集引擎"),
    SCHEMA_DISCOVERY_FAILED(1008, "Schema发现失败"),
    ;

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
