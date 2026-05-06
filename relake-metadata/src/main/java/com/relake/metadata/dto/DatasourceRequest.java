package com.relake.metadata.dto;

import lombok.Data;

@Data
public class DatasourceRequest {

    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String password;
    private String extraParams;
    private String description;
}
