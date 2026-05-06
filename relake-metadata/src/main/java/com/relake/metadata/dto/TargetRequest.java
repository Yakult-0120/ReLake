package com.relake.metadata.dto;

import lombok.Data;

@Data
public class TargetRequest {

    private String name;
    private String storageType;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region;
    private String paimonWarehouse;
    private String description;
}
