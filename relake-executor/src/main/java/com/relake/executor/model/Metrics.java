package com.relake.executor.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Job 运行指标
 */
@Data
@Accessors(chain = true)
public class Metrics {

    /** 已读取记录数 */
    private long recordsIn;

    /** 已输出记录数 */
    private long recordsOut;

    /** 已读取字节数 */
    private long bytesIn;

    /** 已输出字节数 */
    private long bytesOut;

    /** 错误记录数 */
    private long errorCount;

    /** 当前延迟(ms) */
    private long latencyMs;

    public static Metrics empty() {
        return new Metrics();
    }
}
