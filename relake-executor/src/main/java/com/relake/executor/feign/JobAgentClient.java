package com.relake.executor.feign;

import com.relake.common.web.R;
import com.relake.executor.dto.JobStatusVO;
import com.relake.executor.dto.JobSubmitRequest;
import com.relake.executor.dto.JobSubmitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Job Agent Feign 客户端 — executor 调 Agent 的通用 CLI 任务接口
 */
@FeignClient(
        name = "relake-job-agent",
        path = "/api/v1/jobs",
        configuration = JobAgentClientConfig.class
)
public interface JobAgentClient {

    @PostMapping
    R<JobSubmitResponse> submitJob(@RequestBody JobSubmitRequest request);

    @GetMapping("/{jobId}")
    R<JobStatusVO> getJobStatus(@PathVariable("jobId") String jobId);

    @PostMapping("/{jobId}/stop")
    R<Void> stopJob(@PathVariable("jobId") String jobId);
}
