package com.relake.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.relake.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_target")
public class Target extends BaseEntity {

    /** 目标名称 */
    private String name;

    /** 存储类型: MINIO */
    private String storageType;

    /** MinIO地址 */
    private String endpoint;

    /** AccessKey */
    private String accessKey;

    /** SecretKey(AES加密存储) */
    private String secretKey;

    /** Bucket名称 */
    private String bucket;

    /** Region */
    private String region;

    /** Paimon Warehouse路径 */
    private String paimonWarehouse;

    /** 状态: ACTIVE, DISABLED */
    private String status;

    /** 备注 */
    private String description;
}
