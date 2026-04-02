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
public class ConditionGroup<T> {

    private final List<ConditionElement> elements = new ArrayList<>();
    private JoinType defaultJoinType = JoinType.AND;

    public ConditionGroup<T> and(Consumer<ConditionGroup<T>> subGroup) {
        ConditionGroup<T> group = new ConditionGroup<>();
        subGroup.accept(group);
        this.elements.add(new GroupElement(JoinType.AND, group));
        return this;
    }

    public ConditionGroup<T> or(Consumer<ConditionGroup<T>> subGroup) {
        ConditionGroup<T> group = new ConditionGroup<>();
        subGroup.accept(group);
        this.elements.add(new GroupElement(JoinType.OR, group));
        return this;
    }

    // 新增：括号优先级控制方法
    public ConditionGroup<T> group(Consumer<ConditionGroup<T>> subGroup) {
        ConditionGroup<T> group = new ConditionGroup<>();
        subGroup.accept(group);
        this.elements.add(new GroupElement(this.defaultJoinType, group));
        return this;
    }

    public ConditionGroup<T> eq(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "=", value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> eq(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notEq(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "<>", value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notEq(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "<>", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> gt(String column, Object value) {
        this.elements.add(new SimpleCondition(column, ">", value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> gt(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, ">", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> lt(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "<", value, this.defaultJoinType));
        return this;
    }


    public <R> ConditionGroup<T> lt(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "<", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> ge(String column, Object value) {
        this.elements.add(new SimpleCondition(column, ">=", value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> ge(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, ">=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> le(String column, Object value) {
        this.elements.add(new SimpleCondition(column, "<=", value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> le(TypeFunction<T, R> field, Object value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "<=", value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> like(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> like(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notLike(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> leftLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> leftLike(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notLeftLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notLeftLike(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", "%" + value, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> rightLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> rightLike(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notRightLike(String column, String value) {
        this.elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notRightLike(TypeFunction<T, R> field, String value) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(column, "NOT LIKE", value + "%", this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> in(String column, List<?> values) {
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> in(TypeFunction<T, R> field, List<?> values) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notIn(String column, List<?> values) {
        this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notIn(TypeFunction<T, R> field, List<?> values) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> betweenAnd(String column, Object value1, Object value2) {
        this.elements.add(new BetweenCondition(column, value1, value2, false, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> betweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new BetweenCondition(column, value1, value2, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notBetweenAnd(String column, Object value1, Object value2) {
        this.elements.add(new BetweenCondition(column, value1, value2, true, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notBetweenAnd(TypeFunction<T, R> field, Object value1, Object value2) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new BetweenCondition(column, value1, value2, true, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> isNull(String column) {
        this.elements.add(new NullCondition(column, true, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> isNull(TypeFunction<T, R> field) {
        String columnName = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new NullCondition(columnName, true, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> isNotNull(String column) {
        this.elements.add(new NullCondition(column, false, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> isNotNull(TypeFunction<T, R> field) {
        String columnName = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new NullCondition(columnName, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> and() {
        this.defaultJoinType = JoinType.AND;
        return this;
    }

    public ConditionGroup<T> or() {
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
}
