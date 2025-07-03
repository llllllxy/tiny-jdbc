package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.sql.condition.*;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 *     条件构造器
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
        this.elements.add(new GroupElement(JoinType.AND, group));
        return this;
    }

    public ConditionGroup or(Consumer<ConditionGroup> subGroup) {
        ConditionGroup group = new ConditionGroup();
        subGroup.accept(group);
        this.elements.add(new GroupElement(JoinType.OR, group));
        return this;
    }

    // 新增：括号优先级控制方法
    public ConditionGroup group(Consumer<ConditionGroup> subGroup) {
        ConditionGroup group = new ConditionGroup();
        subGroup.accept(group);
        this.elements.add(new GroupElement(this.defaultJoinType, group));
        return this;
    }

    public ConditionGroup eq(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "=", value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup eq(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup notEq(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "!=", value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notEq(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "!=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup gt(String column, Object value) {
        this.elements.add(new SimpleCondition(column, ">", value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup gt(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, ">", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup lt(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "<", value, this.defaultJoinType));
        return this;
    }


    public <T, R> ConditionGroup lt(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "<", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup ge(String column, Object value) {
        this.elements.add(new SimpleCondition(column, ">=", value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup ge(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, ">=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup le(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "<=", value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup le(TypeFunction<T, R> field, Object value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "<=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup like(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup like(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup notLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notLike(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup leftLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup leftLike(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup notLeftLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notLeftLike(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup rightLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup rightLike(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup notRightLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notRightLike(TypeFunction<T, R> field, String value) {
        String column = this.getColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup in(String column, List<?> values) {
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup in(TypeFunction<T, R> field, List<?> values) {
        String column = this.getColumnName(field);
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup notIn(String column, List<?> values) {
        this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notIn(TypeFunction<T, R> field, List<?> values) {
        String column = this.getColumnName(field);
        this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        return this;
    }

    public ConditionGroup betweenAnd(String column, Object value1, Object value2) {
        this.elements.add(new BetweenCondition(column, value1, value2, false, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup betweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = this.getColumnName(field);
        this.elements.add(new BetweenCondition(column, value1, value2, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup notBetweenAnd(String column, Object value1, Object value2) {
        this.elements.add(new BetweenCondition(column, value1, value2, true, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup notBetweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = this.getColumnName(field);
        this.elements.add(new BetweenCondition(column, value1, value2, true, this.defaultJoinType));
        return this;
    }


    public ConditionGroup isNull(String column) {
        this.elements.add(new NullCondition(column, true, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup isNull(TypeFunction<T, R> field) {
        String columnName = this.getColumnName(field);
        this.elements.add(new NullCondition(columnName, true, this.defaultJoinType));
        return this;
    }


    public ConditionGroup isNotNull(String column) {
        this.elements.add(new NullCondition(column, false, this.defaultJoinType));
        return this;
    }

    public <T, R> ConditionGroup isNotNull(TypeFunction<T, R> field) {
        String columnName = this.getColumnName(field);
        this.elements.add(new NullCondition(columnName, false, this.defaultJoinType));
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
        return this.elements.isEmpty();
    }

    public String toSql() {
        if (this.elements.isEmpty()) {
            return "";
        }
        StringBuilder sql = new StringBuilder();
        sql.append(this.elements.get(0).toSql());
        for (int i = 1; i < this.elements.size(); i++) {
            sql.append(" ").append(this.elements.get(i).getJoinType().getSql()).append(" ");
            sql.append(this.elements.get(i).toSql());
        }
        return sql.toString();
    }

    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (ConditionElement element : this.elements) {
            params.addAll(element.getParameters());
        }
        return params;
    }

    private <T, R> String getColumnName(TypeFunction<T, R> field) {
        return LambdaUtils.getLambdaColumnName(field);
    }
}
