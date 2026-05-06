package com.relake.job.agent.controller;

import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.job.agent.core.JobRegistry;
import com.relake.job.agent.core.JobRunner;
import com.relake.job.agent.model.JobStatusVO;
import com.relake.job.agent.model.JobSubmitRequest;
import com.relake.job.agent.model.JobSubmitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Job Agent REST 控制器 — 通用 CLI 任务执行
 * <p>
 * 端点路径 /api/v1/jobs，走 Nacos 服务发现直连，不走 Gateway。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobAgentController {

    private final JobRunner runner;
    private final JobRegistry registry;

    /**
     * 提交 CLI Job
     */
    @PostMapping
    public R<JobSubmitResponse> submitJob(@RequestBody JobSubmitRequest request) {
        String jobId = "job-" + request.getTaskId() + "-" + System.currentTimeMillis();
        log.info("收到 Job 提交: jobId={}, taskId={}, command={} {}",
                jobId, request.getTaskId(), request.getCommand(),
                request.getArgs() != null ? String.join(" ", request.getArgs()) : "");

        runner.submit(jobId, request);

        JobSubmitResponse resp = new JobSubmitResponse();
        resp.setJobId(jobId);
        resp.setStatus("SUBMITTED");
        return R.ok(resp);
    }

    /**
     * 查询 Job 状态
     */
    @GetMapping("/{jobId}")
    public R<JobStatusVO> getJobStatus(@PathVariable String jobId) {
        JobStatusVO status = registry.get(jobId);
        if (status == null) {
            return R.fail(ResultCode.NOT_FOUND, "Job 不存在: " + jobId);
        }
        return R.ok(status);
    }

    /**
     * 停止 Job
     */
    @PostMapping("/{jobId}/stop")
    public R<Void> stopJob(@PathVariable String jobId) {
        log.info("收到停止请求: jobId={}", jobId);
        boolean stopped = runner.stop(jobId);
        if (!stopped) {
            JobStatusVO existing = registry.get(jobId);
            if (existing == null) {
                return R.fail(ResultCode.NOT_FOUND, "Job 不存在: " + jobId);
            }
            existing.setStatus("STOPPED");
        }
        return R.ok();
    }
}
