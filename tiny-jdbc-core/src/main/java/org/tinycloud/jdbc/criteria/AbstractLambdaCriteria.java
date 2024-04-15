package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * 查询条件构造器-抽象类（lambda版）
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:40
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractLambdaCriteria<T, Children extends AbstractLambdaCriteria<T, Children>> extends Criteria<T> {

    /**
     * 占位符
     */
    protected final Children typedThis = (Children) this;

    public <R> Children lt(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orLt(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children lte(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orLte(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children gt(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orGt(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children gte(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orGte(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children eq(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orEq(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children notEq(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orNotEq(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children isNull(TypeFunction<T, ?> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children orIsNull(TypeFunction<T, ?> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children isNotNull(TypeFunction<T, ?> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children orIsNotNull(TypeFunction<T, ?> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children in(TypeFunction<T, ?> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" AND ").append(columnName).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return typedThis;
    }

    public <R> Children orIn(TypeFunction<T, ?> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(columnName).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return typedThis;
    }

    public <R> Children notIn(TypeFunction<T, ?> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" AND ").append(columnName).append(" NOT IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return typedThis;
    }

    public <R> Children orNotIn(TypeFunction<T, ?> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(columnName).append(" NOT IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return typedThis;
    }

    public <R> Children like(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children orLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children notLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children orNotLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children leftLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children orLeftLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children notLeftLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children orNotLeftLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children rightLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children orRightLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children notRightLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children orNotRightLike(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children between(TypeFunction<T, ?> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " AND " + "(" + columnName + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children orBetween(TypeFunction<T, ?> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " OR " + "(" + columnName + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children notBetween(TypeFunction<T, ?> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " AND " + "(" + columnName + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children orNotBetween(TypeFunction<T, ?> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " OR " + "(" + columnName + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public Children and(Consumer<Children> consumer) {
        final Children instance = instance();
        consumer.accept(instance);

        String condition = " AND " + instance.children();
        conditions.add(condition);
        parameters.addAll(instance.parameters);
        return typedThis;
    }

    public Children or(Consumer<Children> consumer) {
        final Children instance = instance();
        consumer.accept(instance);

        String condition = " OR " + instance.children();
        conditions.add(condition);
        parameters.addAll(instance.parameters);
        return typedThis;
    }

    /**
     * 子类返回一个自己的新对象
     */
    protected abstract Children instance();

    public String getColumnName(TypeFunction<T, ?> field) {
        String fieldName = LambdaUtils.getLambdaColumnName(field);
        return fieldName;
    }
}
