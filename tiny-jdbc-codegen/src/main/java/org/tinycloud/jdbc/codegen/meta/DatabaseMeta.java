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


/**
 * <p>
 * 数据库元数据类，封装数据库的元数据信息
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-03-21 11:22
 */
public class DatabaseMeta {
    private final DataSourceConfig dataSourceConfig;

    public DatabaseMeta(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }


    /**
     * 获取数据库中所有表的元数据信息
     * <p>
     * 该方法通过JDBC连接数据库，读取数据库元数据，获取所有用户表的信息，
     * 包括表名、表备注以及每个表的列信息。
     * </p>
     *
     * @return 表元数据列表，包含数据库中所有表的详细信息
     * @throws Exception 当加载数据库驱动、建立数据库连接或读取元数据失败时抛出异常
     */
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

    /**
     * 获取指定表的所有列的元数据信息
     * <p>
     * 该方法通过JDBC连接数据库，读取数据库元数据，获取指定表的所有列的信息，
     * 包括列名、数据类型、列大小、小数位数、是否为空、列的注释、列的顺序。
     * </p>
     *
     * @param metaData  数据库元数据对象
     * @param catalog   数据库的目录名
     * @param schema    数据库的架构名
     * @param tableName 表名
     */
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


    /**
     * 获取数据库的目录名
     * <p>
     * 该方法通过JDBC连接数据库，获取数据库的目录名。
     * </p>
     *
     * @param connection 数据库连接对象
     * @return 数据库的目录名
     */
    private String safeCatalog(Connection connection) {
        try {
            return connection.getCatalog();
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * 获取数据库的架构名
     * <p>
     * 该方法通过JDBC连接数据库，获取数据库的架构名。
     * </p>
     *
     * @param connection 数据库连接对象
     * @return 数据库的架构名
     */
    private String safeSchema(Connection connection) {
        try {
            return connection.getSchema();
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * 获取结果集的指定列的值，如果列不存在则返回null
     *
     * @param rs         结果集对象
     * @param columnLabel 列标签
     * @return 列的值，如果列不存在则返回null
     */
    private String getStringSafely(ResultSet rs, String columnLabel) {
        try {
            return rs.getString(columnLabel);
        } catch (SQLException e) {
            return null;
        }
    }
}