package com.relake.integration.feign;

import com.relake.common.web.R;
import com.relake.integration.dto.DatasourceDTO;
import com.relake.integration.dto.TargetDTO;
import com.relake.integration.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Metadata 服务 Feign 客户端 — 内部调用获取解密凭据
 */
@FeignClient(
        name = "relake-metadata",
        path = "/internal",
        configuration = FeignConfig.class
)
public interface MetadataClient {

    /**
     * 获取数据源完整信息（含解密密码）
     */
    @GetMapping("/datasources/{id}")
    R<DatasourceDTO> getDatasource(@PathVariable("id") Long id);

    /**
     * 获取目标存储完整信息（含解密 SecretKey）
     */
    @GetMapping("/targets/{id}")
    R<TargetDTO> getTarget(@PathVariable("id") Long id);
}
