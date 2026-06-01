package com.relake.metadata.dto;

import lombok.Data;

@Data
public class AclRequest {
    private String principal;
    private String resourceType;
    private String resourceName;
    private String operation;
    private String permissionType = "ALLOW";
    private String host = "*";
}
