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

    public static final Map<String, String> LAMBDA_CACHE = new ConcurrentHashMap<>();

    public <T, R> LambdaCriteria lt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " < " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orLt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " < " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria lte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orLte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria gt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " > " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orGt(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " > " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria gte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " >= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orGte(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " >= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria eq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " = " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " = " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " <> " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orNotEq(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " <> " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria isNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orIsNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria isNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orIsNotNull(TypeFunction<T, R> field) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria in(TypeFunction<T, R> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" AND ").append(columnName).append(" IN (");
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

    public <T, R> LambdaCriteria orIn(TypeFunction<T, R> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(columnName).append(" IN (");
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
        condition.append(" AND ").append(columnName).append(" NOT IN (");
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

    public <T, R> LambdaCriteria orNotIn(TypeFunction<T, R> field, List<R> values) {
        String columnName = getColumnName(field);
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(columnName).append(" NOT IN (");
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
        String condition = " AND " + columnName + " LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orNotLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria leftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE '%" + value + "'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE '%" + value + "'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE '%" + value + "'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orNotLeftLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE '%" + value + "'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria rightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " LIKE '" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " LIKE '" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " AND " + columnName + " NOT LIKE '" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orNotRightLike(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        String condition = " OR " + columnName + " NOT LIKE '" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria between(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " AND " + "(" + columnName + " BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orBetween(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " OR " + "(" + columnName + " BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria notBetween(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " AND " + "(" + columnName + " NOT BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <T, R> LambdaCriteria orNotBetween(TypeFunction<T, R> field, R start, R end) {
        String columnName = getColumnName(field);
        String condition = " OR " + "(" + columnName + " NOT BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public <R> LambdaCriteria and(LambdaCriteria criteria) {
        String condition = " AND " + criteria.children();
        conditions.add(condition);
        return this;
    }

    public <R> LambdaCriteria or(LambdaCriteria criteria) {
        String condition = " OR " + criteria.children();
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
