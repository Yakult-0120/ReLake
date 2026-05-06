package com.relake.job.agent.config;

import com.relake.job.agent.filter.InternalAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Job Agent 配置
 */
@Configuration
public class JobAgentConfig {

    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilterRegistration(
            @Value("${relake.internal-auth.enabled:true}") boolean enabled) {
        FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new InternalAuthFilter(enabled));
        registration.addUrlPatterns("/api/v1/jobs/*");
        registration.setOrder(1);
        return registration;
    }
}
