package org.tinycloud.jdbc.criteria;

import java.util.List;

/**
 * 条件构造器
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class Criteria extends AbstractCriteria {

    public <R> Criteria select(String field) {
        selectFields.add(field);
        return this;
    }

    public <R> Criteria lt(String field, R value) {
        String condition = " AND " + field + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orLt(String field, R value) {
        String condition = " OR " + field + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria lte(String field, R value) {
        String condition = " AND " + field + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orLte(String field, R value) {
        String condition = " OR " + field + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria gt(String field, R value) {
        String condition = " AND " + field + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orGt(String field, R value) {
        String condition = " OR " + field + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria gte(String field, R value) {
        String condition = " AND " + field + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orGte(String field, R value) {
        String condition = " OR " + field + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria eq(String field, R value) {
        String condition = " AND " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orEq(String field, R value) {
        String condition = " OR " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria notEq(String field, R value) {
        String condition = " AND " + field + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria orNotEq(String field, R value) {
        String condition = " OR " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return this;
    }

    public <R> Criteria isNull(String field) {
        String condition = " AND " + field + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria orIsNull(String field) {
        String condition = " OR " + field + " IS NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria isNotNull(String field) {
        String condition = " AND " + field + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria orIsNotNull(String field) {
        String condition = " OR " + field + " IS NOT NULL";
        conditions.add(condition);
        return this;
    }

    public <R> Criteria in(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(" AND ").append(field).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return this;
    }

    public <R> Criteria orIn(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(field).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return this;
    }

    public <R> Criteria notIn(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(" AND ").append(field).append(" NOT IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return this;
    }

    public <R> Criteria orNotIn(String field, List<R> values) {
        StringBuilder condition = new StringBuilder();
        condition.append(" OR ").append(field).append(" NOT IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                condition.append(", ");
            }
            condition.append("?");
        }
        condition.append(")");
        conditions.add(condition.toString());
        parameters.addAll(values);
        return this;
    }

    public <R> Criteria like(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return this;
    }

    public <R> Criteria orLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return this;
    }

    public <R> Criteria notLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return this;
    }

    public <R> Criteria orNotLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return this;
    }

    public <R> Criteria leftLike(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return this;
    }

    public <R> Criteria orLeftLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return this;
    }

    public <R> Criteria notLeftLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return this;
    }

    public <R> Criteria orNotLeftLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return this;
    }

    public <R> Criteria rightLike(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return this;
    }

    public <R> Criteria orRightLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return this;
    }

    public <R> Criteria notRightLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return this;
    }

    public <R> Criteria orNotRightLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return this;
    }

    public <R> Criteria between(String field, R start, R end) {
        String condition = " AND " + "(" + field + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    public <R> Criteria orBetween(String field, R start, R end) {
        String condition = " OR " + "(" + field + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    public <R> Criteria notBetween(String field, R start, R end) {
        String condition = " AND " + "(" + field + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    public <R> Criteria orNotBetween(String field, R start, R end) {
        String condition = " OR " + "(" + field + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return this;
    }

    public <R> Criteria and(Criteria criteria) {
        String condition = " AND " + criteria.children();
        conditions.add(condition);
        parameters.addAll(criteria.parameters);
        return this;
    }

    public <R> Criteria or(Criteria criteria) {
        String condition = " OR " + criteria.children();
        conditions.add(condition);
        parameters.addAll(criteria.parameters);
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