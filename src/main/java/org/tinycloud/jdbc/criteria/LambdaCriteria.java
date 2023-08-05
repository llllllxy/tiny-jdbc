package org.tinycloud.jdbc.criteria;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 条件构造器（Lambda版）
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class LambdaCriteria extends AbstractCriteria {

    public static final Map<String, String> cache = new ConcurrentHashMap<>();

    public <T, R> LambdaCriteria lt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " < " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria lte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " <= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria gt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " > " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria gte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " >= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria equal(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " = " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notEqual(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " <> " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria isNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = columnName + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria isNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = columnName + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria in(TypeFunction<T, R> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(columnName).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append(formatValue(values.get(i)));
        }
        condition.append(")");
        conditions.add(condition.toString());
        return this;
    }

    public <T, R> LambdaCriteria notIn(TypeFunction<T, R> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(columnName).append(" NOT IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append(formatValue(values.get(i)));
        }
        condition.append(")");
        conditions.add(condition.toString());
        return this;
    }

    public <T, R> LambdaCriteria like(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = columnName + " NOT LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria betweenAnd(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = "(" + columnName + " BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notBetweenAnd(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = "(" + columnName + " NOT BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orderBy(TypeFunction<T, R> field, boolean desc) {
        String columnName = getColumnName(field);
        if (desc) {
            columnName += " DESC";
        }
        orderBy.add(columnName);
        return this;
    }

    public <T, R> LambdaCriteria orderBy(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        orderBy.add(columnName);
        return this;
    }

    private <T, R> String getColumnName(TypeFunction<T, R> field) {
        String fieldName = TypeFunction.getLambdaColumnName(field);
        return fieldName;
    }
}
