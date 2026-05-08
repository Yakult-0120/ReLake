package com.relake.integration;

import com.relake.executor.ExecutorApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.relake.integration", "com.relake.executor", "com.relake.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ExecutorApplication.class
        )
)
@MapperScan("com.relake.integration.mapper")
@EnableFeignClients(basePackages = "com.relake.integration.feign")
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }
}
