package com.relake.metadata.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源 VO —— 不返回明文密码
 */
@Data
public class DatasourceVO {

    private Long id;
    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String extraParams;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 从 Entity 转换 */
    public static DatasourceVO from(com.relake.metadata.entity.Datasource ds) {
        DatasourceVO vo = new DatasourceVO();
        vo.setId(ds.getId());
        vo.setName(ds.getName());
        vo.setDbType(ds.getDbType());
        vo.setHost(ds.getHost());
        vo.setPort(ds.getPort());
        vo.setDbName(ds.getDbName());
        vo.setUsername(ds.getUsername());
        vo.setExtraParams(ds.getExtraParams());
        vo.setStatus(ds.getStatus());
        vo.setDescription(ds.getDescription());
        vo.setCreateTime(ds.getCreateTime());
        vo.setUpdateTime(ds.getUpdateTime());
        return vo;
    }
}
