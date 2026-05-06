package com.relake.metadata.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源内部 VO — 含解密后的 password，仅供 InternalController 使用
 */
@Data
public class DatasourceInternalVO {

    private Long id;
    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String password;      // 已解密
    private String extraParams;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
