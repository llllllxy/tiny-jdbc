package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.exception.TinyJdbcException;

/**
 * <p>
 * 受信任的原始 SQL 标记。
 * </p>
 * <p>
 * 框架对所有「标识符」位置（表名、列名、别名、条件字段）默认做严格白名单校验，
 * 以阻断 SQL 注入。少数场景需要传入框架无法识别为标识符的自由 SQL 片段时，
 * 应通过 {@link #wrap(String)} 显式包裹，从而<b>跳过默认的标识符校验</b>——
 * 这表示调用方已确认该内容可信。
 * </p>
 * <p>
 * 典型用途：
 * </p>
 * <ul>
 *   <li>{@code last(RawSql.wrap("FOR UPDATE"))} —— 追加受限的尾部子句时显式授权。</li>
 *   <li>{@code SQL.raw("...")} —— 在需要受信片段的上下文中显式构造。</li>
 * </ul>
 *
 * @author liuxingyu01
 * @since 2026-09-01
 */
public final class RawSql {

    private final String sql;

    private RawSql(String sql) {
        this.sql = sql;
    }

    /**
     * 包裹一段受信任的原始 SQL 片段。
     *
     * @param sql 原始 SQL 片段
     * @return 受信标记实例
     */
    public static RawSql wrap(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new TinyJdbcException("RawSql fragment cannot be null or empty.");
        }
        return new RawSql(sql);
    }

    /**
     * @return 原始的 SQL 片段
     */
    public String sql() {
        return sql;
    }

    @Override
    public String toString() {
        return sql;
    }
}
