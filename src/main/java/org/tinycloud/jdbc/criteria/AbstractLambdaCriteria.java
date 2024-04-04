package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.List;

/**
 * <p>
 *     查询条件构造器-抽象类（lambda版）
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:40
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractLambdaCriteria<Children extends AbstractLambdaCriteria<Children>> extends Criteria {

    /**
     * 占位符
     */
    protected final Children typedThis = (Children) this;

    public <T, R> Children lt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orLt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children lte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orLte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children gt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orGt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children gte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orGte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children eq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children notEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children orNotEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <T, R> Children isNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <T, R> Children orIsNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <T, R> Children isNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <T, R> Children orIsNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <T, R> Children in(TypeFunction<T, R> field, List<R> values) {
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

    public <T, R> Children orIn(TypeFunction<T, R> field, List<R> values) {
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

    public <T, R> Children notIn(TypeFunction<T, R> field, List<R> values) {
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

    public <T, R> Children orNotIn(TypeFunction<T, R> field, List<R> values) {
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

    public <T, R> Children like(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <T, R> Children orLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <T, R> Children notLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <T, R> Children orNotLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <T, R> Children leftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <T, R> Children orLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <T, R> Children notLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <T, R> Children orNotLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <T, R> Children rightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <T, R> Children orRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <T, R> Children notRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <T, R> Children orNotRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <T, R> Children between(TypeFunction<T, R> field, R start, R end) {
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

    public <T, R> Children orBetween(TypeFunction<T, R> field, R start, R end) {
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

    public <T, R> Children notBetween(TypeFunction<T, R> field, R start, R end) {
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

    public <T, R> Children orNotBetween(TypeFunction<T, R> field, R start, R end) {
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

    public <R> Children and(Children criteria) {
        String condition = " AND " + criteria.children();
        conditions.add(condition);
        parameters.addAll(criteria.parameters);
        return typedThis;
    }

    public <R> Children or(Children criteria) {
        String condition = " OR " + criteria.children();
        conditions.add(condition);
        parameters.addAll(criteria.parameters);
        return typedThis;
    }

    public <T, R> String getColumnName(TypeFunction<T, R> field) {
        String fieldName = LambdaUtils.getLambdaColumnName(field);
        return fieldName;
    }
}
