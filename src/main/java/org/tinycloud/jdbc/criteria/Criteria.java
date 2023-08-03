package org.tinycloud.jdbc.criteria;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件构造器
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class Criteria extends AbstractCriteria {

    private final List<String> conditions;
    private String orderBy;

    public Criteria() {
        this.conditions = new ArrayList<>();
        this.orderBy = null;
    }

    public <R> Criteria lt(String field, R value) {
        String condition = field + " < " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria lte(String field, R value) {
        String condition = field + " <= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria gt(String field, R value) {
        String condition = field + " > " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria gte(String field, R value) {
        String condition = field + " >= " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria equal(String field, R value) {
        String condition = field + " = " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria notEqual(String field, R value) {
        String condition = field + " <> " + formatValue(value);
        conditions.add(condition);
        return this;
    }

    public <R> Criteria isNull(String field) {
        String condition = field + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria isNotNull(String field) {
        String condition = field + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria in(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(field).append(" IN (");
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

    public <R> Criteria notIn(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(field).append(" NOT IN (");
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

    public <R> Criteria like(String field, R value) {
        String condition = field + " LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria notLike(String field, R value) {
        String condition = field + " NOT LIKE '%" + value + "%'";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria betweenAnd(String field, R start, R end) {
        String condition = "(" + field + " BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public Criteria orderBy(String field, boolean desc) {
        orderBy = " ORDER BY " + field;
        if (desc) {
            orderBy += " DESC";
        }
        return this;
    }

    public String generateSql() {
        StringBuilder sql = new StringBuilder();
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sql.append(" AND ");
                }
                sql.append(conditions.get(i));
            }
        }
        if (orderBy != null) {
            sql.append(orderBy);
        }
        return sql.toString();
    }
}
