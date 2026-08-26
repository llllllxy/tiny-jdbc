package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.sql.condition.*;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

    public ConditionGroup<T> in(String column, Collection<?> values) {
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> in(TypeFunction<T, R> field, Collection<?> values) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notIn(String column, Collection<?> values) {
        this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notIn(TypeFunction<T, R> field, Collection<?> values) {
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

    // ------------------------ 条件子查询 ------------------------

    public ConditionGroup<T> in(String column, SQL<?> subQuery) {
        this.elements.add(new SubQueryCondition(column, "IN", subQuery, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> in(TypeFunction<T, R> field, SQL<?> subQuery) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SubQueryCondition(column, "IN", subQuery, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notIn(String column, SQL<?> subQuery) {
        this.elements.add(new SubQueryCondition(column, "NOT IN", subQuery, this.defaultJoinType));
        return this;
    }

    public <R> ConditionGroup<T> notIn(TypeFunction<T, R> field, SQL<?> subQuery) {
        String column = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SubQueryCondition(column, "NOT IN", subQuery, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> exists(SQL<?> subQuery) {
        this.elements.add(new SubQueryCondition(null, "EXISTS", subQuery, this.defaultJoinType));
        return this;
    }

    public ConditionGroup<T> notExists(SQL<?> subQuery) {
        this.elements.add(new SubQueryCondition(null, "NOT EXISTS", subQuery, this.defaultJoinType));
        return this;
    }

    /**
     * 自定义操作符的等值条件（可用于列到列比较，value 传 FieldReference）。
     */
    public ConditionGroup<T> and(String column, String opt, Object value) {
        this.elements.add(new SimpleCondition(column, opt, value, this.defaultJoinType));
        return this;
    }

    /**
     * 自定义操作符的等值条件（Lambda 列），value 可为值（参数化）或 FieldReference（列引用）。
     */
    public <R> ConditionGroup<T> and(TypeFunction<T, R> field, String opt, Object value) {
        String columnName = LambdaUtils.getLambdaColumnName(field);
        this.elements.add(new SimpleCondition(columnName, opt, value, this.defaultJoinType));
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

    // ------------------------ 条件判断（IfAbsent，值为空则跳过） ------------------------

    /**
     * 默认判空断言：null / 空串 / 空集合 / 空 Map → false（跳过该条件）。
     */
    static <T> Predicate<T> getDefaultPredicate() {
        return value -> {
            if (value == null) {
                return false;
            }
            if (value instanceof String) {
                return !((String) value).trim().isEmpty();
            }
            if (value instanceof Collection) {
                return !((Collection<?>) value).isEmpty();
            }
            if (value instanceof Map) {
                return !((Map<?, ?>) value).isEmpty();
            }
            return true;
        };
    }

    /**
     * 内部：判空谓词为真才添加条件（集中"空则跳过"，避免重复）。
     */
    private ConditionGroup<T> addIf(Predicate<Object> predicate, Object value, Function<Object, ConditionElement> builder) {
        if (predicate.test(value)) {
            this.elements.add(builder.apply(value));
        }
        return this;
    }

    // ------ 块级开关：整组条件按开关启用 / 禁用 ------

    public ConditionGroup<T> and(boolean condition, Consumer<ConditionGroup<T>> subGroup) {
        return condition ? and(subGroup) : this;
    }

    public ConditionGroup<T> and(BooleanSupplier condition, Consumer<ConditionGroup<T>> subGroup) {
        return condition.getAsBoolean() ? and(subGroup) : this;
    }

    public ConditionGroup<T> or(boolean condition, Consumer<ConditionGroup<T>> subGroup) {
        return condition ? or(subGroup) : this;
    }

    public ConditionGroup<T> or(BooleanSupplier condition, Consumer<ConditionGroup<T>> subGroup) {
        return condition.getAsBoolean() ? or(subGroup) : this;
    }

    // ------ 比较类 ------

    public ConditionGroup<T> eqIfAbsent(String column, Object value) {
        return eqIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> eqIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "=", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> eqIfAbsent(TypeFunction<T, R> field, Object value) {
        return eqIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> eqIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return eqIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> notEqIfAbsent(String column, Object value) {
        return notEqIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> notEqIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "<>", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> notEqIfAbsent(TypeFunction<T, R> field, Object value) {
        return notEqIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> notEqIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return notEqIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> gtIfAbsent(String column, Object value) {
        return gtIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> gtIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, ">", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> gtIfAbsent(TypeFunction<T, R> field, Object value) {
        return gtIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> gtIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return gtIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> geIfAbsent(String column, Object value) {
        return geIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> geIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, ">=", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> geIfAbsent(TypeFunction<T, R> field, Object value) {
        return geIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> geIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return geIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> ltIfAbsent(String column, Object value) {
        return ltIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> ltIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "<", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> ltIfAbsent(TypeFunction<T, R> field, Object value) {
        return ltIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> ltIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return ltIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> leIfAbsent(String column, Object value) {
        return leIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> leIfAbsent(String column, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "<=", v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> leIfAbsent(TypeFunction<T, R> field, Object value) {
        return leIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> leIfAbsent(TypeFunction<T, R> field, Object value, Predicate<Object> predicate) {
        return leIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    // ------ LIKE 类 ------

    public ConditionGroup<T> likeIfAbsent(String column, String value) {
        return likeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> likeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "LIKE", "%" + v + "%", this.defaultJoinType));
    }

    public <R> ConditionGroup<T> likeIfAbsent(TypeFunction<T, R> field, String value) {
        return likeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> likeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return likeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> notLikeIfAbsent(String column, String value) {
        return notLikeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> notLikeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "NOT LIKE", "%" + v + "%", this.defaultJoinType));
    }

    public <R> ConditionGroup<T> notLikeIfAbsent(TypeFunction<T, R> field, String value) {
        return notLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> notLikeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return notLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> leftLikeIfAbsent(String column, String value) {
        return leftLikeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> leftLikeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "LIKE", "%" + v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> leftLikeIfAbsent(TypeFunction<T, R> field, String value) {
        return leftLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> leftLikeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return leftLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> notLeftLikeIfAbsent(String column, String value) {
        return notLeftLikeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> notLeftLikeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "NOT LIKE", "%" + v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> notLeftLikeIfAbsent(TypeFunction<T, R> field, String value) {
        return notLeftLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> notLeftLikeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return notLeftLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> rightLikeIfAbsent(String column, String value) {
        return rightLikeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> rightLikeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "LIKE", v + "%", this.defaultJoinType));
    }

    public <R> ConditionGroup<T> rightLikeIfAbsent(TypeFunction<T, R> field, String value) {
        return rightLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> rightLikeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return rightLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    public ConditionGroup<T> notRightLikeIfAbsent(String column, String value) {
        return notRightLikeIfAbsent(column, value, getDefaultPredicate());
    }

    public ConditionGroup<T> notRightLikeIfAbsent(String column, String value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, "NOT LIKE", v + "%", this.defaultJoinType));
    }

    public <R> ConditionGroup<T> notRightLikeIfAbsent(TypeFunction<T, R> field, String value) {
        return notRightLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value);
    }

    public <R> ConditionGroup<T> notRightLikeIfAbsent(TypeFunction<T, R> field, String value, Predicate<Object> predicate) {
        return notRightLikeIfAbsent(LambdaUtils.getLambdaColumnName(field), value, predicate);
    }

    // ------ IN 类（值集合为空则跳过） ------

    public ConditionGroup<T> inIfAbsent(String column, Collection<?> values) {
        return inIfAbsent(column, values, getDefaultPredicate());
    }

    public ConditionGroup<T> inIfAbsent(String column, Collection<?> values, Predicate<Collection<?>> predicate) {
        if (predicate.test(values)) {
            this.elements.add(new InCondition(column, values, false, this.defaultJoinType));
        }
        return this;
    }

    public <R> ConditionGroup<T> inIfAbsent(TypeFunction<T, R> field, Collection<?> values) {
        return inIfAbsent(LambdaUtils.getLambdaColumnName(field), values);
    }

    public <R> ConditionGroup<T> inIfAbsent(TypeFunction<T, R> field, Collection<?> values, Predicate<Collection<?>> predicate) {
        return inIfAbsent(LambdaUtils.getLambdaColumnName(field), values, predicate);
    }

    public ConditionGroup<T> notInIfAbsent(String column, Collection<?> values) {
        return notInIfAbsent(column, values, getDefaultPredicate());
    }

    public ConditionGroup<T> notInIfAbsent(String column, Collection<?> values, Predicate<Collection<?>> predicate) {
        if (predicate.test(values)) {
            this.elements.add(new InCondition(column, values, true, this.defaultJoinType));
        }
        return this;
    }

    public <R> ConditionGroup<T> notInIfAbsent(TypeFunction<T, R> field, Collection<?> values) {
        return notInIfAbsent(LambdaUtils.getLambdaColumnName(field), values);
    }

    public <R> ConditionGroup<T> notInIfAbsent(TypeFunction<T, R> field, Collection<?> values, Predicate<Collection<?>> predicate) {
        return notInIfAbsent(LambdaUtils.getLambdaColumnName(field), values, predicate);
    }

    // ------ BETWEEN 类（左边界为空则跳过） ------

    public ConditionGroup<T> betweenAndIfAbsent(String column, Object value1, Object value2) {
        return betweenAndIfAbsent(column, value1, value2, getDefaultPredicate());
    }

    public ConditionGroup<T> betweenAndIfAbsent(String column, Object value1, Object value2, Predicate<Object> predicate) {
        return addIf(predicate, value1, v -> new BetweenCondition(column, v, value2, false, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> betweenAndIfAbsent(TypeFunction<T, R> field, Object value1, Object value2) {
        return betweenAndIfAbsent(LambdaUtils.getLambdaColumnName(field), value1, value2);
    }

    public <R> ConditionGroup<T> betweenAndIfAbsent(TypeFunction<T, R> field, Object value1, Object value2, Predicate<Object> predicate) {
        return betweenAndIfAbsent(LambdaUtils.getLambdaColumnName(field), value1, value2, predicate);
    }

    public ConditionGroup<T> notBetweenAndIfAbsent(String column, Object value1, Object value2) {
        return notBetweenAndIfAbsent(column, value1, value2, getDefaultPredicate());
    }

    public ConditionGroup<T> notBetweenAndIfAbsent(String column, Object value1, Object value2, Predicate<Object> predicate) {
        return addIf(predicate, value1, v -> new BetweenCondition(column, v, value2, true, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> notBetweenAndIfAbsent(TypeFunction<T, R> field, Object value1, Object value2) {
        return notBetweenAndIfAbsent(LambdaUtils.getLambdaColumnName(field), value1, value2);
    }

    public <R> ConditionGroup<T> notBetweenAndIfAbsent(TypeFunction<T, R> field, Object value1, Object value2, Predicate<Object> predicate) {
        return notBetweenAndIfAbsent(LambdaUtils.getLambdaColumnName(field), value1, value2, predicate);
    }

    // ------ 自定义操作符（andIfAbsent） ------

    public ConditionGroup<T> andIfAbsent(String column, String opt, Object value) {
        return andIfAbsent(column, opt, value, getDefaultPredicate());
    }

    public ConditionGroup<T> andIfAbsent(String column, String opt, Object value, Predicate<Object> predicate) {
        return addIf(predicate, value, v -> new SimpleCondition(column, opt, v, this.defaultJoinType));
    }

    public <R> ConditionGroup<T> andIfAbsent(TypeFunction<T, R> field, String opt, Object value) {
        return andIfAbsent(LambdaUtils.getLambdaColumnName(field), opt, value);
    }

    public <R> ConditionGroup<T> andIfAbsent(TypeFunction<T, R> field, String opt, Object value, Predicate<Object> predicate) {
        return andIfAbsent(LambdaUtils.getLambdaColumnName(field), opt, value, predicate);
    }
}
