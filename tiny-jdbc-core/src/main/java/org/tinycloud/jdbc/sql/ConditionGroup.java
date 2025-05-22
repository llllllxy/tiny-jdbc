package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:05
 */
public class ConditionGroup {

    private final List<ConditionElement> elements = new ArrayList<>();
    private JoinType defaultJoinType = JoinType.AND;

    public ConditionGroup and(Consumer<ConditionGroup> subGroup) {
        ConditionGroup group = new ConditionGroup();
        subGroup.accept(group);
        elements.add(new GroupElement(JoinType.AND, group));
        return this;
    }

    public ConditionGroup or(Consumer<ConditionGroup> subGroup) {
        ConditionGroup group = new ConditionGroup();
        subGroup.accept(group);
        elements.add(new GroupElement(JoinType.OR, group));
        return this;
    }

    // 新增：括号优先级控制方法
    public ConditionGroup group(Consumer<ConditionGroup> subGroup) {
        ConditionGroup group = new ConditionGroup();
        subGroup.accept(group);
        elements.add(new GroupElement(defaultJoinType, group));
        return this;
    }

    public ConditionGroup eq(String column, Object value) {
        elements.add(new SimpleCondition(column, "=", value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup eq(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "=", value, defaultJoinType));
        return this;
    }

    public ConditionGroup notEq(String column, Object value) {
        elements.add(new SimpleCondition(column, "!=", value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notEq(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "!=", value, defaultJoinType));
        return this;
    }

    public ConditionGroup gt(String column, Object value) {
        elements.add(new SimpleCondition(column, ">", value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup gt(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, ">", value, defaultJoinType));
        return this;
    }

    public ConditionGroup lt(String column, Object value) {
        elements.add(new SimpleCondition(column, "<", value, defaultJoinType));
        return this;
    }


    public <T, R> ConditionGroup lt(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "<", value, defaultJoinType));
        return this;
    }

    public ConditionGroup ge(String column, Object value) {
        elements.add(new SimpleCondition(column, ">=", value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup ge(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, ">=", value, defaultJoinType));
        return this;
    }

    public ConditionGroup le(String column, Object value) {
        elements.add(new SimpleCondition(column, "<=", value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup le(TypeFunction<T, R> field, Object value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "<=", value, defaultJoinType));
        return this;
    }

    public ConditionGroup like(String column, String value) {
        elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup like(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", defaultJoinType));
        return this;
    }

    public ConditionGroup notLike(String column, String value) {
        elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notLike(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", defaultJoinType));
        return this;
    }

    public ConditionGroup leftLike(String column, String value) {
        elements.add(new SimpleCondition(column, "LIKE", value + "%", defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup leftLike(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "LIKE", value + "%", defaultJoinType));
        return this;
    }

    public ConditionGroup notLeftLike(String column, String value) {
        elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notLeftLike(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", defaultJoinType));
        return this;
    }

    public ConditionGroup rightLike(String column, String value) {
        elements.add(new SimpleCondition(column, "LIKE", "%" + value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup rightLike(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "LIKE", "%" + value, defaultJoinType));
        return this;
    }

    public ConditionGroup notRightLike(String column, String value) {
        elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notRightLike(TypeFunction<T, R> field, String value) {
        String column = getColumnName(field);
        elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, defaultJoinType));
        return this;
    }

    public ConditionGroup in(String column, List<?> values) {
        elements.add(new InCondition(column, values, false, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup in(TypeFunction<T, R> field, List<?> values) {
        String column = getColumnName(field);
        elements.add(new InCondition(column, values, false, defaultJoinType));
        return this;
    }

    public ConditionGroup notIn(String column, List<?> values) {
        elements.add(new InCondition(column, values, true, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notIn(TypeFunction<T, R> field, List<?> values) {
        String column = getColumnName(field);
        elements.add(new InCondition(column, values, true, defaultJoinType));
        return this;
    }

    public ConditionGroup betweenAnd(String column, Object value1, Object value2) {
        elements.add(new BetweenCondition(column, value1, value2, false, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup betweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = getColumnName(field);
        elements.add(new BetweenCondition(column, value1, value2, false, defaultJoinType));
        return this;
    }

    public ConditionGroup notBetweenAnd(String column, Object value1, Object value2) {
        elements.add(new BetweenCondition(column, value1, value2, true, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notBetweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = getColumnName(field);
        elements.add(new BetweenCondition(column, value1, value2, true, defaultJoinType));
        return this;
    }


    public ConditionGroup isNull(String column) {
        elements.add(new NullCondition(column, true, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup isNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        elements.add(new NullCondition(columnName, true, defaultJoinType));
        return this;
    }


    public ConditionGroup isNotNull(String column) {
        elements.add(new NullCondition(column, false, defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup isNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        elements.add(new NullCondition(columnName, false, defaultJoinType));
        return this;
    }

    public ConditionGroup and() {
        this.defaultJoinType = JoinType.AND;
        return this;
    }

    public ConditionGroup or() {
        this.defaultJoinType = JoinType.OR;
        return this;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public String toSql() {
        if (elements.isEmpty()) {
            return "";
        }
        StringBuilder sql = new StringBuilder();
        sql.append(elements.get(0).toSql());
        for (int i = 1; i < elements.size(); i++) {
            sql.append(" ").append(elements.get(i).getJoinType().getSql()).append(" ");
            sql.append(elements.get(i).toSql());
        }
        return sql.toString();
    }

    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (ConditionElement element : elements) {
            params.addAll(element.getParameters());
        }
        return params;
    }

    private <T, R> String getColumnName(TypeFunction<T, R> field) {
        return LambdaUtils.getLambdaColumnName(field);
    }
}
