package org.tinycloud.jdbc.sql.enums;

/**
 * <p>
 *     内部枚举：连接类型
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:01
 */
public enum JoinType {
    AND("AND"),
    OR("OR");

    private String sql;

    public String getSql() {
        return sql;
    }

    JoinType(String sql) {
        this.sql = sql;
    }
}
