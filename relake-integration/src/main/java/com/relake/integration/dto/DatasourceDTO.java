package com.relake.integration.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源内部 VO（从 Metadata 服务获取，含解密密码）
 */
@Data
public class DatasourceDTO {

    private Long id;
    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String password;      // 内部已解密
    private String extraParams;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
