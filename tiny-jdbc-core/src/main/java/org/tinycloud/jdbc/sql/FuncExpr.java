package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

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
 * <p>
 *     表达式字符串无法用 {@code ?} 参数化，因此框架内部严格区分两条路径：
 * </p>
 * <ul>
 *   <li>{@link #raw(String)} —— 调用方提供的<b>列引用</b>，按列引用白名单校验；</li>
 *   <li>{@link #fragment(String)} —— 框架自身按受信方式拼装的片段（如 {@code NOW()}、
 *       {@code DISTINCT col}），内容不含未校验的外部输入。</li>
 * </ul>
 * <p>
 *     若使用方确需传入不受限的自由表达式，请走 {@link #of(RawSql)} 显式授权。
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
     * 构建一个无参数的「列引用」表达式，按列引用白名单校验。
     * <p>用于 SELECT 字段、函数参数等由调用方提供列名的位置。</p>
     *
     * @param expression 列引用（如 {@code u.name}、{@code *}）
     * @return 表达式节点
     */
    static FuncExpr raw(String expression) {
        SqlIdentifierUtils.checkColumnRef(expression);
        return new FuncExpr(expression, Collections.emptyList());
    }

    /**
     * 构建一个无参数的「受信片段」表达式，<b>不做标识符校验</b>。
     * <p>
     * 仅供框架内部使用：片段由框架自行拼装（常量、整型字面量、已校验列名），
     * 不包含任何未校验的外部输入。使用方请改用 {@link #of(RawSql)}。
     * </p>
     *
     * @param expression 受信 SQL 片段
     * @return 表达式节点
     */
    static FuncExpr fragment(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new TinyJdbcException("FuncExpr fragment cannot be null or empty.");
        }
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
     * <p>函数名会被原样拼接，因此按标识符白名单校验。</p>
     */
    static FuncExpr call(String funcName, FuncExpr... args) {
        SqlIdentifierUtils.checkFunctionName(funcName);
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
     * <p>仅供框架内部按受信方式构造；参数列表会做防御性拷贝。</p>
     */
    static FuncExpr ofRaw(String expression, List<Object> params) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new TinyJdbcException("FuncExpr ofRaw expression cannot be null or empty.");
        }
        return new FuncExpr(expression, params == null
                ? Collections.<Object>emptyList()
                : new ArrayList<>(params));
    }

    /**
     * 设置别名。别名会被原样拼接到 {@code AS} 之后，因此按别名白名单校验。
     *
     * @param alias 别名
     * @return 当前表达式对象，支持链式调用
     */
    public FuncExpr as(String alias) {
        SqlIdentifierUtils.checkAlias(alias);
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
     * <p>返回只读视图，调用方无法通过它改动表达式内部状态。</p>
     *
     * @return 参数列表（不可变）
     */
    public List<Object> getParameters() {
        return Collections.unmodifiableList(params);
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
     * @deprecated 该入口会原样拼接表达式、绕过校验。请改用 {@link #of(RawSql)} 显式表达信任，
     *             或需要列引用时使用 {@link FuncBuilder#col(String)}。本方法现按列引用校验，
     *             非列引用表达式将直接报错。计划在 2.1.0 移除。
     */
    @Deprecated
    public static FuncExpr of(String expression) {
        return raw(expression);
    }

    /**
     * 创建一个通用表达式，用 {@link RawSql} 显式授权原始片段（不受限）。
     *
     * @param expression 受信任的原始 SQL 片段
     * @return FuncExpr 对象
     */
    public static FuncExpr of(RawSql expression) {
        if (expression == null) {
            throw new TinyJdbcException("RawSql expression cannot be null.");
        }
        return fragment(expression.sql());
    }
}
