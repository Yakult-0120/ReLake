package com.relake.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局配置 — 解决 Snowflake Long ID 精度丢失问题
 * <p>
 * JavaScript number 安全范围仅 16 位 (2^53-1)，
 * MyBatis-Plus ASSIGN_ID 生成的 Snowflake ID 为 19 位 Long，
 * 序列化为 JSON string 即可跨语言无损传递。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
        };
    }
}
