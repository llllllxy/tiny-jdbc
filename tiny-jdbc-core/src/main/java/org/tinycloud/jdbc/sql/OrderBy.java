package org.tinycloud.jdbc.sql;

/**
 * <p>
 *     排序
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:00
 */
public class OrderBy {
    String column;
    boolean isDesc;

    public OrderBy(String column, boolean isDesc) {
        this.column = column;
        this.isDesc = isDesc;
    }
}
