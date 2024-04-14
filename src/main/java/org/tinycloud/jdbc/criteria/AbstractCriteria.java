package org.tinycloud.jdbc.criteria;


import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * 查询条件构造器-抽象类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:39
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractCriteria<T, Children extends AbstractCriteria<T, Children>> extends Criteria<T> {

    /**
     * 占位符
     */
    protected final Children typedThis = (Children) this;

    public <R> Children lt(String field, R value) {
        String condition = " AND " + field + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orLt(String field, R value) {
        String condition = " OR " + field + " < " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children lte(String field, R value) {
        String condition = " AND " + field + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orLte(String field, R value) {
        String condition = " OR " + field + " <= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children gt(String field, R value) {
        String condition = " AND " + field + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orGt(String field, R value) {
        String condition = " OR " + field + " > " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children gte(String field, R value) {
        String condition = " AND " + field + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orGte(String field, R value) {
        String condition = " OR " + field + " >= " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children eq(String field, R value) {
        String condition = " AND " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orEq(String field, R value) {
        String condition = " OR " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children notEq(String field, R value) {
        String condition = " AND " + field + " <> " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children orNotEq(String field, R value) {
        String condition = " OR " + field + " = " + "?";
        conditions.add(condition);
        parameters.add(value);
        return typedThis;
    }

    public <R> Children isNull(String field) {
        String condition = " AND " + field + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children orIsNull(String field) {
        String condition = " OR " + field + " IS NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children isNotNull(String field) {
        String condition = " AND " + field + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children orIsNotNull(String field) {
        String condition = " OR " + field + " IS NOT NULL";
        conditions.add(condition);
        return typedThis;
    }

    public <R> Children in(String field, List<R> values) {
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
        return typedThis;
    }

    public <R> Children orIn(String field, List<R> values) {
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
        return typedThis;
    }

    public <R> Children notIn(String field, List<R> values) {
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
        return typedThis;
    }

    public <R> Children orNotIn(String field, List<R> values) {
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
        return typedThis;
    }

    public <R> Children like(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children orLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children notLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children orNotLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value + "%");
        return typedThis;
    }

    public <R> Children leftLike(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children orLeftLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children notLeftLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children orNotLeftLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add("%" + value);
        return typedThis;
    }

    public <R> Children rightLike(String field, R value) {
        String condition = " AND " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children orRightLike(String field, R value) {
        String condition = " OR " + field + " LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children notRightLike(String field, R value) {
        String condition = " AND " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children orNotRightLike(String field, R value) {
        String condition = " OR " + field + " NOT LIKE ?";
        conditions.add(condition);
        parameters.add(value + "%");
        return typedThis;
    }

    public <R> Children between(String field, R start, R end) {
        String condition = " AND " + "(" + field + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children orBetween(String field, R start, R end) {
        String condition = " OR " + "(" + field + " BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children notBetween(String field, R start, R end) {
        String condition = " AND " + "(" + field + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children orNotBetween(String field, R start, R end) {
        String condition = " OR " + "(" + field + " NOT BETWEEN " +
                "?" +
                " AND " +
                "?" + ")";
        conditions.add(condition);
        parameters.add(start);
        parameters.add(end);
        return typedThis;
    }

    public <R> Children and(Consumer<Children> consumer) {
        final Children instance = instance();
        consumer.accept(instance);

        String condition = " AND " + instance.children();
        conditions.add(condition);
        parameters.addAll(instance.parameters);
        return typedThis;
    }

    public <R> Children or(Consumer<Children> consumer) {
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
}
