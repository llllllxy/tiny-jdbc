package org.tinycloud.jdbc.criteria;


/**
 * <p>
 *     原生更新表达式值包装类
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18 12:21
 */
public class RawUpdateSqlValue {
    private final String sqlExpression;

    public RawUpdateSqlValue(String sqlExpression) {
        this.sqlExpression = sqlExpression;
    }

    public String getSqlExpression() {
        return sqlExpression;
    }
}
