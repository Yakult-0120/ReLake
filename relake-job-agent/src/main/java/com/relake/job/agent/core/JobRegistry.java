package com.relake.job.agent.core;

import com.relake.job.agent.model.JobStatusVO;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Job 内存注册表 — 管理所有运行中/已完成的 Job 状态
 * <p>
 * 纯内存存储，Agent 重启后所有状态丢失（可接受）。
 * 数据同步任务的状态真正记录在 integration 的 ds_task 表中。
 */
@Component
public class JobRegistry {

    private final ConcurrentHashMap<String, JobStatusVO> jobMap = new ConcurrentHashMap<>();

    public void register(String jobId, JobStatusVO status) {
        jobMap.put(jobId, status);
    }

    public JobStatusVO get(String jobId) {
        return jobMap.get(jobId);
    }

    public void remove(String jobId) {
        jobMap.remove(jobId);
    }

    public void update(String jobId, String status, String error, Integer exitCode) {
        JobStatusVO existing = jobMap.get(jobId);
        if (existing != null) {
            existing.setStatus(status);
            if (error != null) existing.setErrorMessage(error);
            if (exitCode != null) existing.setExitCode(exitCode);
        }
    }

    public void incrementOutputLines(String jobId, long delta) {
        JobStatusVO existing = jobMap.get(jobId);
        if (existing != null) {
            existing.setOutputLines(existing.getOutputLines() + delta);
        }
    }

    public void incrementErrorLines(String jobId, long delta) {
        JobStatusVO existing = jobMap.get(jobId);
        if (existing != null) {
            existing.setErrorLines(existing.getErrorLines() + delta);
        }
    }
}
