package com.relake.integration.config;

import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * Feign 全局配置 — 内部调用注入标识头
 */
@Slf4j
public class FeignConfig {

    /**
     * 注入 X-Internal-Call 头，标识当前请求为服务间内部调用
     */
    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Call", "true");
            log.debug("Feign 调用: {} {}", requestTemplate.method(), requestTemplate.url());
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
