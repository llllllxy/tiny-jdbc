package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.condition.SimpleCondition;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.sql.enums.SqlJoinType;
import org.tinycloud.jdbc.util.LambdaUtils;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     SQL JOIN 结构：{@code LEFT JOIN table alias ON cond1 AND cond2 ...}
 * </p>
 * <p>
 *     ON 条件复用 {@link SimpleCondition}，因此操作符白名单、值为 null 时的
 *     {@code IS NULL} / {@code IS NOT NULL} 归一规则与 WHERE 条件完全一致。
 *     除 {@code CROSS JOIN} 外，JOIN 必须至少有一个 ON 条件，否则渲染时报错。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public class Join {
    private final String table;
    private final String alias;
    private final SqlJoinType joinType;
    private final List<SimpleCondition> onConditions = new ArrayList<>();

    public Join(String table, String alias, SqlJoinType joinType) {
        SqlIdentifierUtils.checkTableName(table);
        if (alias != null && !alias.trim().isEmpty()) {
            SqlIdentifierUtils.checkAlias(alias);
        }
        this.table = table;
        this.alias = alias;
        this.joinType = joinType;
    }

    /**
     * 列到列连接条件：{@code field1 = field2}（右侧为字段引用，不参数化）。
     */
    public Join on(String field1, String field2) {
        this.onConditions.add(new SimpleCondition(field1, "=", new FieldReference(field2), JoinType.AND));
        return this;
    }

    /**
     * 带操作符的连接条件：{@code field1 opt value}。操作符须命中白名单。
     */
    public Join on(String field1, String opt, Object value) {
        this.onConditions.add(new SimpleCondition(field1, opt, value, JoinType.AND));
        return this;
    }

    /**
     * 带操作符的连接条件，并用 {@link RawSql} 显式授权操作符（跳过白名单）。
     */
    public Join on(String field1, RawSql opt, Object value) {
        this.onConditions.add(new SimpleCondition(field1, opt, value, JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（列到列）。
     */
    public Join and(String field1, String field2) {
        this.onConditions.add(new SimpleCondition(field1, "=", new FieldReference(field2), JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（带操作符）。操作符须命中白名单。
     */
    public Join and(String field1, String opt, Object value) {
        this.onConditions.add(new SimpleCondition(field1, opt, value, JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（带操作符），并用 {@link RawSql} 显式授权操作符（跳过白名单）。
     */
    public Join and(String field1, RawSql opt, Object value) {
        this.onConditions.add(new SimpleCondition(field1, opt, value, JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（带操作符），若 value 为 null 或空字符串则跳过该条件。
     */
    public Join andIfAbsent(String field1, String opt, Object value) {
        if (isBlank(value)) {
            return this;
        }
        return and(field1, opt, value);
    }

    /**
     * 首个连接条件（带操作符），若 value 为 null 或空字符串则跳过该条件。
     */
    public Join onIfAbsent(String field1, String opt, Object value) {
        if (isBlank(value)) {
            return this;
        }
        return on(field1, opt, value);
    }

    // ------ Lambda（TypeFunction）重载 ------

    /**
     * 列到列连接条件（Lambda）：{@code field1 = field2}（右侧为字段引用，不参数化）。
     */
    public <T1, R1, T2, R2> Join on(TypeFunction<T1, R1> field1, TypeFunction<T2, R2> field2) {
        this.onConditions.add(new SimpleCondition(columnName(field1), "=",
                new FieldReference(columnName(field2)), JoinType.AND));
        return this;
    }

    /**
     * 带操作符的连接条件（Lambda）：{@code field1 opt value}。操作符须命中白名单。
     */
    public <T, R> Join on(TypeFunction<T, R> field1, String opt, Object value) {
        this.onConditions.add(new SimpleCondition(columnName(field1), opt, value, JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（Lambda，列到列）。
     */
    public <T1, R1, T2, R2> Join and(TypeFunction<T1, R1> field1, TypeFunction<T2, R2> field2) {
        this.onConditions.add(new SimpleCondition(columnName(field1), "=",
                new FieldReference(columnName(field2)), JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（Lambda，带操作符）。操作符须命中白名单。
     */
    public <T, R> Join and(TypeFunction<T, R> field1, String opt, Object value) {
        this.onConditions.add(new SimpleCondition(columnName(field1), opt, value, JoinType.AND));
        return this;
    }

    /**
     * 追加 AND 条件（Lambda，带操作符），若 value 为 null 或空字符串则跳过该条件。
     */
    public <T, R> Join andIfAbsent(TypeFunction<T, R> field1, String opt, Object value) {
        if (isBlank(value)) {
            return this;
        }
        return and(field1, opt, value);
    }

    /**
     * 首个连接条件（Lambda，带操作符），若 value 为 null 或空字符串则跳过该条件。
     */
    public <T, R> Join onIfAbsent(TypeFunction<T, R> field1, String opt, Object value) {
        if (isBlank(value)) {
            return this;
        }
        return on(field1, opt, value);
    }

    private static String columnName(TypeFunction<?, ?> field) {
        return LambdaUtils.getLambdaColumnName(field);
    }

    private static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        return false;
    }

    public String getTable() {
        return table;
    }

    public String getAlias() {
        return alias;
    }

    public SqlJoinType getJoinType() {
        return joinType;
    }

    /**
     * 生成 JOIN 子句 SQL。
     * <p>
     * 除 {@code CROSS JOIN} 外，缺少 ON 条件会生成非法 SQL（如 {@code LEFT JOIN t a}），
     * 因此在渲染期显式报错，而不是把错误留到数据库执行。
     * </p>
     */
    public String toSql() {
        if (this.joinType != SqlJoinType.CROSS && this.onConditions.isEmpty()) {
            throw new TinyJdbcException("A " + this.joinType.getSql() + " requires at least one ON condition,"
                    + " but table '" + table + "' has none. Use crossJoin() if a JOIN without ON is intended.");
        }
        StringBuilder sb = new StringBuilder(" ").append(joinType.getSql()).append(" ").append(table);
        if (alias != null && !alias.trim().isEmpty()) {
            sb.append(" ").append(alias);
        }
        if (!onConditions.isEmpty()) {
            sb.append(" ON ");
            for (int i = 0; i < onConditions.size(); i++) {
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append(onConditions.get(i).toSql());
            }
        }
        return sb.toString();
    }

    /**
     * 收集 ON 条件中的参数。
     */
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (SimpleCondition cond : onConditions) {
            params.addAll(cond.getParameters());
        }
        return params;
    }
}
