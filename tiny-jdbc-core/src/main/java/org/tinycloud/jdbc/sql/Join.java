package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.sql.enums.SqlJoinType;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     SQL JOIN 结构：{@code LEFT JOIN table alias ON cond1 AND cond2 ...}
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public class Join {
    private final String table;
    private final String alias;
    private final SqlJoinType joinType;
    private final List<OnCondition> onConditions = new ArrayList<>();

    public Join(String table, String alias, SqlJoinType joinType) {
        this.table = table;
        this.alias = alias;
        this.joinType = joinType;
    }

    /**
     * 列到列连接条件：{@code field1 = field2}（右侧为字段引用，不参数化）。
     */
    public Join on(String field1, String field2) {
        this.onConditions.add(new OnCondition(field1, "=", new FieldReference(field2), true));
        return this;
    }

    /**
     * 带操作符的连接条件：{@code field1 opt value}。
     */
    public Join on(String field1, String opt, Object value) {
        this.onConditions.add(new OnCondition(field1, opt, value, isRef(value)));
        return this;
    }

    /**
     * 追加 AND 条件（列到列）。
     */
    public Join and(String field1, String field2) {
        this.onConditions.add(new OnCondition(field1, "=", new FieldReference(field2), true));
        return this;
    }

    /**
     * 追加 AND 条件（带操作符）。
     */
    public Join and(String field1, String opt, Object value) {
        this.onConditions.add(new OnCondition(field1, opt, value, isRef(value)));
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
        this.onConditions.add(new OnCondition(columnName(field1), "=", new FieldReference(columnName(field2)), true));
        return this;
    }

    /**
     * 带操作符的连接条件（Lambda）：{@code field1 opt value}。
     */
    public <T, R> Join on(TypeFunction<T, R> field1, String opt, Object value) {
        this.onConditions.add(new OnCondition(columnName(field1), opt, value, isRef(value)));
        return this;
    }

    /**
     * 追加 AND 条件（Lambda，列到列）。
     */
    public <T1, R1, T2, R2> Join and(TypeFunction<T1, R1> field1, TypeFunction<T2, R2> field2) {
        this.onConditions.add(new OnCondition(columnName(field1), "=", new FieldReference(columnName(field2)), true));
        return this;
    }

    /**
     * 追加 AND 条件（Lambda，带操作符）。
     */
    public <T, R> Join and(TypeFunction<T, R> field1, String opt, Object value) {
        this.onConditions.add(new OnCondition(columnName(field1), opt, value, isRef(value)));
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
     */
    public String toSql() {
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
        for (OnCondition cond : onConditions) {
            if (!cond.isFieldRef()) {
                params.add(cond.getValue());
            }
        }
        return params;
    }

    private static boolean isRef(Object value) {
        return value instanceof FieldReference;
    }

    /**
     * ON 条件内部结构。
     */
    private static class OnCondition {
        private final String field1;
        private final String opt;
        private final Object value;
        private final boolean fieldRef;

        OnCondition(String field1, String opt, Object value, boolean fieldRef) {
            this.field1 = field1;
            this.opt = opt;
            this.value = value;
            this.fieldRef = fieldRef;
        }

        String toSql() {
            String v = fieldRef ? ((FieldReference) value).getColumn() : "?";
            return field1 + " " + opt + " " + v;
        }

        boolean isFieldRef() {
            return fieldRef;
        }

        Object getValue() {
            return value;
        }
    }
}
