package com.relake.gateway.controller;

import com.relake.common.web.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public R<String> health() {
        return R.ok("ReLake Gateway is running, " + LocalDateTime.now());
    }
}
