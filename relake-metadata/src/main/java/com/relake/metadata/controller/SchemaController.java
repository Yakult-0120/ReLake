package com.relake.metadata.controller;

import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.metadata.dto.TableInfo;
import com.relake.metadata.entity.Datasource;
import com.relake.metadata.service.DatasourceService;
import com.relake.metadata.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final DatasourceService datasourceService;
    private final AesUtil aesUtil;

    /**
     * 获取数据源下的所有表名
     */
    @GetMapping("/{datasourceId}/tables")
    public R<List<String>> getTables(@PathVariable Long datasourceId) {
        Datasource ds = datasourceService.getEntity(datasourceId);
        List<String> tables = new ArrayList<>();
        try (Connection conn = getConnection(ds)) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(ds.getDbName(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (Exception e) {
            log.error("Schema发现失败: datasourceId={}, error={}", datasourceId, e.getMessage());
            return R.fail(ResultCode.SCHEMA_DISCOVERY_FAILED, e.getMessage());
        }
        return R.ok(tables);
    }

    /**
     * 获取指定表的 Schema 详情（列名、类型、是否主键、注释）
     */
    @GetMapping("/{datasourceId}/tables/{tableName}")
    public R<TableInfo> getTableSchema(@PathVariable Long datasourceId, @PathVariable String tableName) {
        Datasource ds = datasourceService.getEntity(datasourceId);
        try (Connection conn = getConnection(ds)) {
            DatabaseMetaData meta = conn.getMetaData();

            // 表注释
            String tableComment = "";
            try (ResultSet rs = meta.getTables(ds.getDbName(), null, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    tableComment = rs.getString("REMARKS");
                }
            }

            // 主键列集合
            Set<String> pkColumns = new HashSet<>();
            try (ResultSet rs = meta.getPrimaryKeys(ds.getDbName(), null, tableName)) {
                while (rs.next()) {
                    pkColumns.add(rs.getString("COLUMN_NAME"));
                }
            }

            // 列信息
            TableInfo info = new TableInfo();
            info.setTableName(tableName);
            info.setTableComment(tableComment);
            List<TableInfo.ColumnInfo> columns = new ArrayList<>();
            try (ResultSet rs = meta.getColumns(ds.getDbName(), null, tableName, null)) {
                while (rs.next()) {
                    TableInfo.ColumnInfo col = new TableInfo.ColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("DATA_TYPE"),
                            "YES".equals(rs.getString("IS_NULLABLE")),
                            pkColumns.contains(rs.getString("COLUMN_NAME")),
                            rs.getString("REMARKS")
                    );
                    columns.add(col);
                }
            }
            info.setColumns(columns);
            return R.ok(info);
        } catch (Exception e) {
            log.error("Schema发现失败: datasourceId={}, table={}, error={}", datasourceId, tableName, e.getMessage());
            return R.fail(ResultCode.SCHEMA_DISCOVERY_FAILED, e.getMessage());
        }
    }

    private Connection getConnection(Datasource ds) throws SQLException {
        String url = "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort() + "/" + ds.getDbName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        String pwd = aesUtil.decrypt(ds.getPassword());
        return DriverManager.getConnection(url, ds.getUsername(), pwd);
    }
}
