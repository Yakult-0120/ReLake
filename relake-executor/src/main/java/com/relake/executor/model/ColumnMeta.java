package com.relake.executor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DataX 列元数据 — 用于生成 hdfswriter column 定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMeta {
    /** 列名 */
    private String name;
    /** DataX 类型: long, double, string, date, boolean, bytes */
    private String type;

    /**
     * 将 MySQL DATA_TYPE 映射为 DataX 类型
     */
    public static String mysqlTypeToDataX(String mysqlType) {
        if (mysqlType == null) return "string";
        String t = mysqlType.toLowerCase();
        if (t.contains("int") || t.contains("serial")) return "long";
        if (t.contains("float") || t.contains("double") ||
            t.contains("decimal") || t.contains("numeric") || t.contains("real")) return "double";
        if (t.contains("date") || t.contains("time") || t.contains("year")) return "date";
        if (t.contains("bool") || t.equals("bit")) return "boolean";
        if (t.contains("blob") || t.contains("binary")) return "bytes";
        return "string"; // varchar, char, text, enum, set, json, etc.
    }
}
