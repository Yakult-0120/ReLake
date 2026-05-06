package com.relake.job.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通用 Job Agent 启动类
 * <p>
 * 部署在安装了目标 CLI 工具（DataX、Sqoop 等）的节点上，
 * 通过 Nacos 注册，接收 executor 的 HTTP 任务提交，
 * 使用 ProcessBuilder 执行任意外部命令并跟踪生命周期。
 */
@SpringBootApplication(scanBasePackages = "com.relake.job.agent")
public class JobAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobAgentApplication.class, args);
    }
}
