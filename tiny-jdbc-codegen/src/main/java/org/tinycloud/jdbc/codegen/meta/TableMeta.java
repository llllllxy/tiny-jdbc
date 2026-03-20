package org.tinycloud.jdbc.codegen.meta;

import java.util.ArrayList;
import java.util.List;

public class TableMeta {
    private String tableName;
    private String remarks;
    private List<ColumnMeta> columns = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<ColumnMeta> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMeta> columns) {
        this.columns = columns;
    }

    public List<ColumnMeta> getPrimaryKeys() {
        List<ColumnMeta> primaryKeys = new ArrayList<>();
        if (columns == null) {
            return primaryKeys;
        }
        for (ColumnMeta column : columns) {
            if (column.isPrimaryKey()) {
                primaryKeys.add(column);
            }
        }
        return primaryKeys;
    }
}