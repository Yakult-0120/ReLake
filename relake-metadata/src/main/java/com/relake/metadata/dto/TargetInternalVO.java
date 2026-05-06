package com.relake.metadata.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 目标存储内部 VO — 含解密后的 secretKey，仅供 InternalController 使用
 */
@Data
public class TargetInternalVO {

    private Long id;
    private String name;
    private String storageType;
    private String endpoint;
    private String accessKey;
    private String secretKey;        // 已解密
    private String bucket;
    private String region;
    private String paimonWarehouse;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
