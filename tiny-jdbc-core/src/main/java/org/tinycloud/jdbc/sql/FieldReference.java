package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 *     字段引用：将右侧当作字段 / 表达式使用（不参数化）。
 * </p>
 * <p>
 *     用于 SET 赋值、条件比较等场景，例如
 *     {@code set("u.email", new FieldReference("a.email"))} 生成 {@code u.email = a.email}。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public class FieldReference {
    private final String column;

    /**
     * @param column 字段 / 表达式（原样拼接，不参数化）
     */
    public FieldReference(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("FieldReference column cannot be null or empty");
        }
        this.column = column;
    }

    /**
     * @return 字段 / 表达式
     */
    public String getColumn() {
        return column;
    }
}
