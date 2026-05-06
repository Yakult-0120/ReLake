package com.relake.executor.model;

/**
 * 引擎 Job 运行状态
 */
public enum JobStatus {

    /** 已提交，等待调度 */
    SUBMITTED,

    /** 运行中 */
    RUNNING,

    /** 正常完成 */
    FINISHED,

    /** 执行失败 */
    FAILED,

    /** 手动停止 */
    STOPPED,

    /** 未知状态（Job不存在或查询失败） */
    UNKNOWN
}
