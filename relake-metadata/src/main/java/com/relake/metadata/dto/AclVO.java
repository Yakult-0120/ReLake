package com.relake.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AclVO {
    private String principal;
    private String resourceType;
    private String resourceName;
    private String operation;
    private String permissionType;
    private String host;
}
