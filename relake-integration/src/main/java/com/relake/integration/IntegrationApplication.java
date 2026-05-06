package com.relake.integration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.relake.integration", "com.relake.executor", "com.relake.common"})
@MapperScan("com.relake.integration.mapper")
@EnableFeignClients(basePackages = "com.relake.integration.feign")
public class IntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }
}
