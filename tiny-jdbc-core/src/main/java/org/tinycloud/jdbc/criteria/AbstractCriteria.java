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
        return this.lt(true, field, value);
    }

    public <R> Children lt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " < " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orLt(String field, R value) {
        return this.orLt(true, field, value);
    }

    public <R> Children orLt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " < " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children lte(String field, R value) {
        return this.lte(true, field, value);
    }

    public <R> Children lte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " <= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orLte(String field, R value) {
        return this.orLte(true, field, value);
    }

    public <R> Children orLte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " <= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gt(String field, R value) {
        return this.gt(true, field, value);
    }

    public <R> Children gt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " > " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orGt(String field, R value) {
        return this.orGt(true, field, value);
    }

    public <R> Children orGt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " > " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gte(String field, R value) {
        return this.gte(true, field, value);
    }

    public <R> Children gte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " >= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orGte(String field, R value) {
        return this.orGte(true, field, value);
    }

    public <R> Children orGte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " >= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children eq(String field, R value) {
        return this.eq(true, field, value);
    }

    public <R> Children eq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " = " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orEq(String field, R value) {
        return this.orEq(true, field, value);
    }

    public <R> Children orEq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " = " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children notEq(String field, R value) {
        return this.notEq(true, field, value);
    }

    public <R> Children notEq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " <> " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orNotEq(String field, R value) {
        return this.orNotEq(true, field, value);
    }

    public <R> Children orNotEq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " <> " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children isNull(String field) {
        return this.isNull(true, field);
    }

    public <R> Children isNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children orIsNull(String field) {
        return this.orIsNull(true, field);
    }

    public <R> Children orIsNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children isNotNull(String field) {
        return this.isNotNull(true, field);
    }

    public <R> Children isNotNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children orIsNotNull(String field) {
        return this.orIsNotNull(true, field);
    }

    public <R> Children orIsNotNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children in(String field, List<R> values) {
        return this.in(true, field, values);
    }

    public <R> Children in(boolean whether, String field, List<R> values) {
        return this.whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(field).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children orIn(String field, List<R> values) {
        return this.orIn(true, field, values);
    }

    public <R> Children orIn(boolean whether, String field, List<R> values) {
        return this.whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(field).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children notIn(String field, List<R> values) {
        return this.notIn(true, field, values);
    }

    public <R> Children notIn(boolean whether, String field, List<R> values) {
        return this.whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(field).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children orNotIn(String field, List<R> values) {
        return this.orNotIn(true, field, values);
    }

    public <R> Children orNotIn(boolean whether, String field, List<R> values) {
        return this.whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(field).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children like(String field, R value) {
        return this.like(true, field, value);
    }

    public <R> Children like(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children orLike(String field, R value) {
        return this.orLike(true, field, value);
    }

    public <R> Children orLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children notLike(String field, R value) {
        return this.notLike(true, field, value);
    }

    public <R> Children notLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children orNotLike(String field, R value) {
        return this.orNotLike(true, field, value);
    }

    public <R> Children orNotLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }


    public <R> Children leftLike(String field, R value) {
        return this.leftLike(true, field, value);
    }

    public <R> Children leftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children orLeftLike(String field, R value) {
        return this.orLeftLike(true, field, value);
    }

    public <R> Children orLeftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children notLeftLike(String field, R value) {
        return this.notLeftLike(true, field, value);
    }

    public <R> Children notLeftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children orNotLeftLike(String field, R value) {
        return this.orNotLeftLike(true, field, value);
    }

    public <R> Children orNotLeftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children rightLike(String field, R value) {
        return this.rightLike(true, field, value);
    }

    public <R> Children rightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children orRightLike(String field, R value) {
        return this.orRightLike(true, field, value);
    }

    public <R> Children orRightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children notRightLike(String field, R value) {
        return this.notRightLike(true, field, value);
    }

    public <R> Children notRightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children orNotRightLike(String field, R value) {
        return this.orNotRightLike(true, field, value);
    }

    public <R> Children orNotRightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }


    public <R> Children between(String field, R start, R end) {
        return this.between(true, field, start, end);
    }

    public <R> Children between(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + "(" + field + " BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children orBetween(String field, R start, R end) {
        return this.orBetween(true, field, start, end);
    }

    public <R> Children orBetween(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + "(" + field + " BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children notBetween(String field, R start, R end) {
        return this.notBetween(true, field, start, end);
    }

    public <R> Children notBetween(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = " AND " + "(" + field + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children orNotBetween(String field, R start, R end) {
        return this.orNotBetween(true, field, start, end);
    }

    public <R> Children orNotBetween(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = " OR " + "(" + field + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children and(Consumer<Children> consumer) {
        return this.and(true, consumer);
    }

    public <R> Children and(boolean whether, Consumer<Children> consumer) {
        return this.whetherDo(whether, () -> {
            final Children instance = this.instance();
            consumer.accept(instance);
            String condition = " AND " + instance.children();
            this.conditions.add(condition);
            this.whereParameters.addAll(instance.whereParameters);
        });
    }

    public <R> Children or(Consumer<Children> consumer) {
        return this.or(true, consumer);
    }

    public <R> Children or(boolean whether, Consumer<Children> consumer) {
        return this.whetherDo(whether, () -> {
            final Children instance = this.instance();
            consumer.accept(instance);
            String condition = " OR " + instance.children();
            this.conditions.add(condition);
            this.whereParameters.addAll(instance.whereParameters);
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
        return this.typedThis;
    }
}
