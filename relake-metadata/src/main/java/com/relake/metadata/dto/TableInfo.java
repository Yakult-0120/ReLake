package com.relake.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Schema 发现结果
 */
@Data
public class TableInfo {

    private String tableName;
    private String tableComment;
    private List<ColumnInfo> columns;

    @Data
    @AllArgsConstructor
    public static class ColumnInfo {
        private String columnName;
        private String columnType;
        private Integer dataType;       // java.sql.Types
        private Integer columnSize;     // 列长度
        private Boolean nullable;
        private Boolean primaryKey;
        private String comment;
    }
}
