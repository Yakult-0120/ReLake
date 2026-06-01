package com.relake.metadata.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 目标存储 VO —— 不返回明文 SecretKey
 */
@Data
public class TargetVO {

    /** 雪花算法 Long ID，序列化为字符串避免 JS 精度丢失 */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String name;
    private String storageType;
    private String endpoint;
    private String accessKey;
    private String bucket;
    private String region;
    private String paimonWarehouse;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static TargetVO from(com.relake.metadata.entity.Target t) {
        TargetVO vo = new TargetVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setStorageType(t.getStorageType());
        vo.setEndpoint(t.getEndpoint());
        vo.setAccessKey(t.getAccessKey());
        vo.setBucket(t.getBucket());
        vo.setRegion(t.getRegion());
        vo.setPaimonWarehouse(t.getPaimonWarehouse());
        vo.setStatus(t.getStatus());
        vo.setDescription(t.getDescription());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        return vo;
    }
}
