package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     函数/表达式节点。
 * </p>
 * <p>
 *     用于构建 SQL 函数表达式（聚合、字符串、日期、条件等），可嵌套组合、可设置别名。
 *     列名 / 裸 SQL 用字符串直接拼接（不参数化），值字面量通过 {@link #lit(Object)} 参数化（? + 占位）。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public class FuncExpr {
    /**
     * 表达式 SQL 片段（可能含 ? 占位，不含别名）
     */
    private final String expression;
    /**
     * 该表达式携带的参数（值为字面量时参数化）
     */
    private final List<Object> params;
    private String alias;

    private FuncExpr(String expression, List<Object> params) {
        this.expression = expression;
        this.params = params;
    }

    /**
     * 构建一个无参数的表达式（列名 / 裸 SQL / 常量）。
     */
    static FuncExpr raw(String expression) {
        return new FuncExpr(expression, Collections.emptyList());
    }

    /**
     * 字面量值 -> 参数化占位（放在?中, 由外部绑定参数）。
     */
    static FuncExpr value(Object value) {
        return new FuncExpr("?", new ArrayList<>(Collections.singletonList(value)));
    }

    /**
     * 调用一个函数：NAME(arg1, arg2, ...)，并合并所有子表达式的参数。
     */
    static FuncExpr call(String funcName, FuncExpr... args) {
        StringBuilder sb = new StringBuilder(funcName).append("(");
        List<Object> merged = new ArrayList<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                FuncExpr arg = args[i];
                if (arg == null) {
                    throw new TinyJdbcException("FuncExpr argument of " + funcName + " cannot be null");
                }
                sb.append(arg.expression);
                merged.addAll(arg.params);
            }
        }
        sb.append(")");
        return new FuncExpr(sb.toString(), merged);
    }

    /**
     * 构建一个带参数的任意表达式（例如 CASE WHEN ... END），用于无法用函数名套用的场景。
     */
    static FuncExpr ofRaw(String expression, List<Object> params) {
        return new FuncExpr(expression, params);
    }

    /**
     * 设置别名。
     *
     * @param alias 别名
     * @return 当前表达式对象，支持链式调用
     */
    public FuncExpr as(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new TinyJdbcException("Alias cannot be null or empty");
        }
        this.alias = alias;
        return this;
    }

    /**
     * 生成表达式 SQL（含别名）。
     *
     * @return 表达式 SQL 字符串
     */
    public String toSql() {
        return alias != null ? expression + " AS " + alias : expression;
    }

    /**
     * 获取该表达式携带的参数。
     *
     * @return 参数列表
     */
    public List<Object> getParameters() {
        return params;
    }

    /**
     * 表达式本体（不含别名），供嵌套拼接。
     */
    String getExpression() {
        return expression;
    }

    /**
     * 创建一个通用表达式（原样字符串）。
     *
     * @param expression SQL 表达式字符串
     * @return FuncExpr 对象
     */
    public static FuncExpr of(String expression) {
        return raw(expression);
    }
}
