package org.tinycloud.jdbc.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 批量插入的构建载体：一份<b>稳定列集</b> + 一组与之顺序一致的行参数。
 *
 * <p>列集在集合内保持一致（多值 INSERT 要求所有行共享同一列），
 * 每行参数数组的长度与 {@link #getColumns()} 严格对应。</p>
 *
 * @author liuxingyu01
 * @since 2026-09-01
 */
public final class BatchInsertSql {

    private final String tableName;
    private final List<String> columns;
    private final List<Object[]> rows;
    private final String primaryKeyColumn;
    private final boolean autoIncrement;

    BatchInsertSql(String tableName, List<String> columns, List<Object[]> rows,
                   String primaryKeyColumn, boolean autoIncrement) {
        this.tableName = tableName;
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.primaryKeyColumn = primaryKeyColumn;
        this.autoIncrement = autoIncrement;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * @return 稳定列集（有序）
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * @return 与 {@link #getColumns()} 一致排序的行参数数组集合
     */
    public List<Object[]> getRows() {
        return rows;
    }

    /**
     * @return 主键列名；未声明 {@code @Id} 时为 null
     */
    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    /**
     * @return {@code true} 表示主键为自增（该列已从列集剔除，由数据库生成，不写回）
     */
    public boolean isAutoIncrement() {
        return autoIncrement;
    }
}
