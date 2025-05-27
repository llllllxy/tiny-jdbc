package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.LambdaUtils;

/**
 * <p>
 * Expression表达式类
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-23 10:07
 */
public class Expression {
    private final String expression;
    private String alias;

    private Expression(String expression) {
        this.expression = expression;
    }

    public Expression as(String alias) {
        this.alias = alias;
        return this;
    }

    @Override
    public String toString() {
        return alias != null ? expression + " AS " + alias : expression;
    }

    // 静态工厂方法：聚合函数
    public static Expression count(String column) {
        return new Expression("COUNT(" + column + ")");
    }

    public static Expression sum(String column) {
        return new Expression("SUM(" + column + ")");
    }

    public static Expression avg(String column) {
        return new Expression("AVG(" + column + ")");
    }

    public static Expression max(String column) {
        return new Expression("MAX(" + column + ")");
    }

    public static Expression min(String column) {
        return new Expression("MIN(" + column + ")");
    }

    // 普通表达式
    public static Expression of(String expression) {
        return new Expression(expression);
    }

    public static <T> Expression of(TypeFunction<T, ?> fields) {
        String expression = LambdaUtils.getLambdaColumnName(fields);
        return new Expression(expression);
    }
}
