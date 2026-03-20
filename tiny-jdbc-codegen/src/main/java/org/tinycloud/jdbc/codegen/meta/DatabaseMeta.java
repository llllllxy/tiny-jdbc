package org.tinycloud.jdbc.codegen.meta;

import org.tinycloud.jdbc.codegen.config.DataSourceConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseMeta {
    private final DataSourceConfig dataSourceConfig;

    public DatabaseMeta(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    public List<TableMeta> getTables() throws Exception {
        Class.forName(dataSourceConfig.getDriverClassName());

        try (Connection connection = DriverManager.getConnection(
                dataSourceConfig.getUrl(),
                dataSourceConfig.getUsername(),
                dataSourceConfig.getPassword())) {

            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = safeCatalog(connection);
            String schema = safeSchema(connection);

            List<TableMeta> tables = new ArrayList<>();

            try (ResultSet rs = metaData.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String remarks = rs.getString("REMARKS");

                    TableMeta tableMeta = new TableMeta();
                    tableMeta.setTableName(tableName);
                    tableMeta.setRemarks(remarks);
                    tableMeta.setColumns(getColumns(metaData, catalog, schema, tableName));

                    tables.add(tableMeta);
                }
            }

            return tables;
        }
    }

    private List<ColumnMeta> getColumns(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
        List<ColumnMeta> columns = new ArrayList<>();
        Map<String, ColumnMeta> columnMap = new LinkedHashMap<>();

        try (ResultSet rs = metaData.getColumns(catalog, schema, tableName, "%")) {
            while (rs.next()) {
                ColumnMeta columnMeta = new ColumnMeta();
                columnMeta.setColumnName(rs.getString("COLUMN_NAME"));
                columnMeta.setDataType(rs.getInt("DATA_TYPE"));
                columnMeta.setTypeName(rs.getString("TYPE_NAME"));
                columnMeta.setColumnSize(rs.getInt("COLUMN_SIZE"));
                columnMeta.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                columnMeta.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columnMeta.setRemarks(rs.getString("REMARKS"));
                columnMeta.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));

                String autoIncrementValue = getStringSafely(rs, "IS_AUTOINCREMENT");
                columnMeta.setAutoIncrement("YES".equalsIgnoreCase(autoIncrementValue));

                columns.add(columnMeta);
                columnMap.put(columnMeta.getColumnName().toUpperCase(), columnMeta);
            }
        }

        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                ColumnMeta columnMeta = columnMap.get(columnName.toUpperCase());
                if (columnMeta != null) {
                    columnMeta.setPrimaryKey(true);
                }
            }
        }

        columns.sort((a, b) -> Integer.compare(a.getOrdinalPosition(), b.getOrdinalPosition()));
        return columns;
    }

    private String safeCatalog(Connection connection) {
        try {
            return connection.getCatalog();
        } catch (SQLException e) {
            return null;
        }
    }

    private String safeSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (SQLException e) {
            return null;
        }
    }

    private String getStringSafely(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException e) {
            return null;
        }
    }
}