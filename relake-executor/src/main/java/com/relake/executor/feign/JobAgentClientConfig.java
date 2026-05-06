package com.relake.executor.feign;

import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Job Agent Feign 客户端配置
 * <p>
 * 注入 X-Internal-Call 认证头 + 超时/重试配置。
 * Agent 端 InternalAuthFilter 校验此头，拒绝外部请求。
 */
@Slf4j
public class JobAgentClientConfig {

    /**
     * 注入 X-Internal-Call 头
     */
    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Call", "true");
            log.debug("Feign 调 Job Agent: {} {}", requestTemplate.method(), requestTemplate.url());
        };
    }

    /**
     * 重试策略：最多 1 次重试，间隔 500ms
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(500, TimeUnit.MILLISECONDS.toMillis(500), 1);
    }
}
