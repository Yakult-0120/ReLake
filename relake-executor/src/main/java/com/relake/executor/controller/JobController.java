package com.relake.executor.controller;

import com.relake.common.web.R;
import com.relake.executor.engine.SyncEngine;
import com.relake.executor.engine.SyncEngineFactory;
import com.relake.executor.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Job 查询端点 — 提供引擎 Job 的状态和指标查询
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final SyncEngineFactory engineFactory;

    /**
     * 根据引擎类型和 Job ID 查询 Job 详情
     */
    @GetMapping("/{jobId}")
    public R<Map<String, Object>> getJob(@PathVariable String jobId,
                                         @RequestParam EngineType engineType) {
        SyncEngine engine = engineFactory.getEngine(engineType);
        JobHandle handle = new JobHandle();
        handle.setJobId(jobId);
        handle.setEngineType(engineType);

        JobStatus status = engine.getStatus(handle);
        Metrics metrics = engine.getMetrics(handle);

        Map<String, Object> result = Map.of(
                "jobId", jobId,
                "engineType", engineType.name(),
                "status", status.name(),
                "metrics", metrics
        );
        return R.ok(result);
    }
}
