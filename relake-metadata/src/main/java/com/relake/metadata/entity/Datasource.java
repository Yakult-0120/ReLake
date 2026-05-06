package com.relake.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.relake.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_datasource")
public class Datasource extends BaseEntity {

    /** 数据源名称 */
    private String name;

    /** 数据库类型: MYSQL, POSTGRESQL */
    private String dbType;

    /** 主机地址 */
    private String host;

    /** 端口 */
    private Integer port;

    /** 数据库名 */
    private String dbName;

    /** 用户名 */
    private String username;

    /** 密码(AES加密存储) */
    private String password;

    /** 额外JDBC参数 */
    private String extraParams;

    /** 状态: ACTIVE, DISABLED */
    private String status;

    /** 备注 */
    private String description;
}
