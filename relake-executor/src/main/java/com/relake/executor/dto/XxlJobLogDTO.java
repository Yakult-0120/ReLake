package com.relake.executor.dto;

import lombok.Data;

/**
 * XXL-JOB 执行日志响应
 */
@Data
public class XxlJobLogDTO {

    /** 日志 ID */
    private int id;

    /** 任务 ID */
    private int jobId;

    /** 执行器地址 */
    private String executorAddress;

    /** Handler 名称 */
    private String executorHandler;

    /** 执行参数 */
    private String executorParam;

    /** 触发时间 */
    private String triggerTime;

    /** 调度结果码 */
    private int triggerCode;

    /** 调度日志 */
    private String triggerMsg;

    /** 执行时间 */
    private String handleTime;

    /** 执行结果码: 0=运行中, 200=成功, 500=失败 */
    private int handleCode;

    /** 执行日志 */
    private String handleMsg;
}
