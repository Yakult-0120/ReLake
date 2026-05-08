package com.relake.integration.model;

/**
 * 同步任务状态枚举
 */
public enum TaskStatus {

    /** 草稿 — 新建未验证 */
    DRAFT,

    /** 校验中 — 正在校验配置 */
    VALIDATING,

    /** 就绪 — 校验通过可启动 */
    READY,

    /** 运行中 */
    RUNNING,

    /** 失败 */
    FAILED,

    /** 已完成 — 同步任务正常结束 */
    FINISHED,

    /** 已停止 */
    STOPPED
}
