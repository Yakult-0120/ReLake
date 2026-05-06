package com.relake.integration.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 目标存储内部 VO（从 Metadata 服务获取，含解密 SecretKey）
 */
@Data
public class TargetDTO {

    private Long id;
    private String name;
    private String storageType;
    private String endpoint;
    private String accessKey;
    private String secretKey;        // 内部已解密
    private String bucket;
    private String region;
    private String paimonWarehouse;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
