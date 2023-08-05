package org.tinycloud.jdbc.criteria;

import java.util.List;

/**
 * 条件构造器
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class Criteria extends AbstractCriteria {

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

    public <R> Criteria notBetweenAnd(String field, R start, R end) {
        String condition = "(" + field + " NOT BETWEEN " +
                formatValue(start) +
                " AND " +
                formatValue(end) + ")";
        conditions.add(condition);
        return this;
    }

    public Criteria orderBy(String field, boolean desc) {
        String orderByString = field;
        if (desc) {
            orderByString += " DESC";
        }
        orderBy.add(orderByString);
        return this;
    }

    public Criteria orderBy(String field) {
        String orderByString = field;
        orderBy.add(orderByString);
        return this;
    }
}
