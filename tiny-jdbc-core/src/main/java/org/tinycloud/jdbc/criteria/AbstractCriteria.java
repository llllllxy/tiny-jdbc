package org.tinycloud.jdbc.criteria;

import java.util.Collections;
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
        return lt(true, field, value);
    }

    public <R> Children lt(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " < " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orLt(String field, R value) {
        return orLt(true, field, value);
    }

    public <R> Children orLt(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " < " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children lte(String field, R value) {
        return lte(true, field, value);
    }

    public <R> Children lte(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " <= " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orLte(String field, R value) {
        return orLte(true, field, value);
    }

    public <R> Children orLte(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " <= " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children gt(String field, R value) {
        return gt(true, field, value);
    }

    public <R> Children gt(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " > " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orGt(String field, R value) {
        return orGt(true, field, value);
    }

    public <R> Children orGt(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " > " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children gte(String field, R value) {
        return gte(true, field, value);
    }

    public <R> Children gte(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " >= " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orGte(String field, R value) {
        return orGte(true, field, value);
    }

    public <R> Children orGte(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " >= " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children eq(String field, R value) {
        return eq(true, field, value);
    }

    public <R> Children eq(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " = " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orEq(String field, R value) {
        return orEq(true, field, value);
    }

    public <R> Children orEq(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " = " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children notEq(String field, R value) {
        return notEq(true, field, value);
    }

    public <R> Children notEq(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " <> " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children orNotEq(String field, R value) {
        return orNotEq(true, field, value);
    }

    public <R> Children orNotEq(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " <> " + "?";
            conditions.add(condition);
            parameters.add(value);
        });
    }

    public <R> Children isNull(String field) {
        return isNull(true, field);
    }

    public <R> Children isNull(boolean whether, String field) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " IS NULL";
            conditions.add(condition);
        });
    }

    public <R> Children orIsNull(String field) {
        return orIsNull(true, field);
    }

    public <R> Children orIsNull(boolean whether, String field) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " IS NULL";
            conditions.add(condition);
        });
    }

    public <R> Children isNotNull(String field) {
        return isNotNull(true, field);
    }

    public <R> Children isNotNull(boolean whether, String field) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " IS NOT NULL";
            conditions.add(condition);
        });
    }

    public <R> Children orIsNotNull(String field) {
        return orIsNotNull(true, field);
    }

    public <R> Children orIsNotNull(boolean whether, String field) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " IS NOT NULL";
            conditions.add(condition);
        });
    }

    public <R> Children in(String field, List<R> values) {
        return in(true, field, values);
    }

    public <R> Children in(boolean whether, String field, List<R> values) {
        return whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(field).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            conditions.add(condition.toString());
            parameters.addAll(values);
        });
    }

    public <R> Children orIn(String field, List<R> values) {
        return orIn(true, field, values);
    }

    public <R> Children orIn(boolean whether, String field, List<R> values) {
        return whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(field).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            conditions.add(condition.toString());
            parameters.addAll(values);
        });
    }

    public <R> Children notIn(String field, List<R> values) {
        return notIn(true, field, values);
    }

    public <R> Children notIn(boolean whether, String field, List<R> values) {
        return whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(field).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            conditions.add(condition.toString());
            parameters.addAll(values);
        });
    }

    public <R> Children orNotIn(String field, List<R> values) {
        return orNotIn(true, field, values);
    }

    public <R> Children orNotIn(boolean whether, String field, List<R> values) {
        return whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(field).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            conditions.add(condition.toString());
            parameters.addAll(values);
        });
    }

    public <R> Children like(String field, R value) {
        return like(true, field, value);
    }

    public <R> Children like(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value + "%");
        });
    }

    public <R> Children orLike(String field, R value) {
        return orLike(true, field, value);
    }

    public <R> Children orLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value + "%");
        });
    }

    public <R> Children notLike(String field, R value) {
        return notLike(true, field, value);
    }

    public <R> Children notLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value + "%");
        });
    }

    public <R> Children orNotLike(String field, R value) {
        return orNotLike(true, field, value);
    }

    public <R> Children orNotLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value + "%");
        });
    }


    public <R> Children leftLike(String field, R value) {
        return leftLike(true, field, value);
    }

    public <R> Children leftLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value);
        });
    }

    public <R> Children orLeftLike(String field, R value) {
        return orLeftLike(true, field, value);
    }

    public <R> Children orLeftLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value);
        });
    }

    public <R> Children notLeftLike(String field, R value) {
        return notLeftLike(true, field, value);
    }

    public <R> Children notLeftLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value);
        });
    }

    public <R> Children orNotLeftLike(String field, R value) {
        return orNotLeftLike(true, field, value);
    }

    public <R> Children orNotLeftLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add("%" + value);
        });
    }

    public <R> Children rightLike(String field, R value) {
        return rightLike(true, field, value);
    }

    public <R> Children rightLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add(value + "%");
        });
    }

    public <R> Children orRightLike(String field, R value) {
        return orRightLike(true, field, value);
    }

    public <R> Children orRightLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            conditions.add(condition);
            parameters.add(value + "%");
        });
    }

    public <R> Children notRightLike(String field, R value) {
        return notRightLike(true, field, value);
    }

    public <R> Children notRightLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add(value + "%");
        });
    }

    public <R> Children orNotRightLike(String field, R value) {
        return orNotRightLike(true, field, value);
    }

    public <R> Children orNotRightLike(boolean whether, String field, R value) {
        return whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            conditions.add(condition);
            parameters.add(value + "%");
        });
    }


    public <R> Children between(String field, R start, R end) {
        return between(true, field, start, end);
    }

    public <R> Children between(boolean whether, String field, R start, R end) {
        return whetherDo(whether, () -> {
            String condition = " AND " + "(" + field + " BETWEEN " + "?" + " AND " + "?" + ")";
            conditions.add(condition);
            parameters.add(start);
            parameters.add(end);
        });
    }

    public <R> Children orBetween(String field, R start, R end) {
        return orBetween(true, field, start, end);
    }

    public <R> Children orBetween(boolean whether, String field, R start, R end) {
        return whetherDo(whether, () -> {
            String condition = " OR " + "(" + field + " BETWEEN " + "?" + " AND " + "?" + ")";
            conditions.add(condition);
            parameters.add(start);
            parameters.add(end);
        });
    }

    public <R> Children notBetween(String field, R start, R end) {
        return notBetween(true, field, start, end);
    }

    public <R> Children notBetween(boolean whether, String field, R start, R end) {
        return whetherDo(whether, () -> {
            String condition = " AND " + "(" + field + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            conditions.add(condition);
            parameters.add(start);
            parameters.add(end);
        });
    }

    public <R> Children orNotBetween(String field, R start, R end) {
        return orNotBetween(true, field, start, end);
    }

    public <R> Children orNotBetween(boolean whether, String field, R start, R end) {
        return whetherDo(whether, () -> {
            String condition = " OR " + "(" + field + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            conditions.add(condition);
            parameters.add(start);
            parameters.add(end);
        });
    }

    public <R> Children and(Consumer<Children> consumer) {
        return and(true, consumer);
    }

    public <R> Children and(boolean whether, Consumer<Children> consumer) {
        return whetherDo(whether, () -> {
            final Children instance = instance();
            consumer.accept(instance);
            String condition = " AND " + instance.children();
            conditions.add(condition);
            parameters.addAll(instance.parameters);
        });
    }

    public <R> Children or(Consumer<Children> consumer) {
        return or(true, consumer);
    }

    public <R> Children or(boolean whether, Consumer<Children> consumer) {
        return whetherDo(whether, () -> {
            final Children instance = instance();
            consumer.accept(instance);
            String condition = " OR " + instance.children();
            conditions.add(condition);
            parameters.addAll(instance.parameters);
        });
    }

    /**
     * 子类返回一个自己的新对象
     */
    protected abstract Children instance();

    /**
     * 函数化的做事
     *
     * @param whether   做不做
     * @param something 做什么
     * @return Children
     */
    protected final Children whetherDo(boolean whether, DoSomething something) {
        if (whether) {
            something.doIt();
        }
        return typedThis;
    }
}
