package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

/**
 * <p>
 *     字段引用：将右侧当作字段 / 表达式使用（不参数化）。
 * </p>
 * <p>
 *     用于 SET 赋值、条件比较等场景，例如
 *     {@code set("u.email", new FieldReference("a.email"))} 生成 {@code u.email = a.email}。
 *     默认按「列引用」白名单校验；若确需传入不受限的表达式，请改用
 *     {@link RawSql} 显式授权。
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
        SqlIdentifierUtils.checkColumnRef(column);
        this.column = column;
    }

    /**
     * @return 字段 / 表达式
     */
    public String getColumn() {
        return column;
    }
}
