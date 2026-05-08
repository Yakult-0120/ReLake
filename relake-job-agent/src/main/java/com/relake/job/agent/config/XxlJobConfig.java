package com.relake.job.agent.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB Executor 配置 — 替代自建 Job Agent 的任务调度
 */
@Configuration
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(
            @Value("${xxl.job.admin.addresses:http://xxl-job-admin:8086/xxl-job-admin}") String adminAddresses,
            @Value("${xxl.job.executor.appname:relake-datax-executor}") String appname,
            @Value("${xxl.job.executor.port:9999}") int port,
            @Value("${xxl.job.executor.logpath:/data/logs/xxl-job}") String logPath,
            @Value("${xxl.job.accessToken:relake_token}") String accessToken) {

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setAccessToken(accessToken);
        return executor;
    }
}
