package org.tinycloud.jdbc.sql.enums;

/**
 * <p>
 *     SQL JOIN 连接类型（区别于 {@link JoinType}，后者是条件连接符 AND/OR）。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public enum SqlJoinType {
    LEFT("LEFT JOIN"),
    RIGHT("RIGHT JOIN"),
    INNER("INNER JOIN"),
    CROSS("CROSS JOIN");

    private final String sql;

    public String getSql() {
        return sql;
    }

    SqlJoinType(String sql) {
        this.sql = sql;
    }
}
