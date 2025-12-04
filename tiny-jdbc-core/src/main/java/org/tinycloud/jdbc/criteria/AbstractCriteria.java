package org.tinycloud.jdbc.criteria;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            String condition = this.getConditionPrefix() + field + " < ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children lte(String field, R value) {
        return this.lte(true, field, value);
    }

    public <R> Children lte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " <= ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gt(String field, R value) {
        return this.gt(true, field, value);
    }

    public <R> Children gt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " > ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gte(String field, R value) {
        return this.gte(true, field, value);
    }

    public <R> Children gte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " >= ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children eq(String field, R value) {
        return this.eq(true, field, value);
    }

    public <R> Children eq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " = ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children notEq(String field, R value) {
        return this.notEq(true, field, value);
    }

    public <R> Children notEq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " <> ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children isNull(String field) {
        return this.isNull(true, field);
    }

    public <R> Children isNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children isNotNull(String field) {
        return this.isNotNull(true, field);
    }

    public <R> Children isNotNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children in(String field, List<R> values) {
        return this.in(true, field, values);
    }

    public <R> Children in(boolean whether, String field, List<R> values) {
        return this.whetherDo(whether, () -> {
            StringBuilder condition = new StringBuilder();
            condition.append(getConditionPrefix())
                    .append(field)
                    .append(" IN (")
                    .append(IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")))
                    .append(")");
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
            condition.append(getConditionPrefix())
                    .append(field)
                    .append(" NOT IN (")
                    .append(IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children like(String field, R value) {
        return this.like(true, field, value);
    }

    public <R> Children like(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children notLike(String field, R value) {
        return this.notLike(true, field, value);
    }

    public <R> Children notLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children leftLike(String field, R value) {
        return this.leftLike(true, field, value);
    }

    public <R> Children leftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children notLeftLike(String field, R value) {
        return this.notLeftLike(true, field, value);
    }

    public <R> Children notLeftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children rightLike(String field, R value) {
        return this.rightLike(true, field, value);
    }

    public <R> Children rightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children notRightLike(String field, R value) {
        return this.notRightLike(true, field, value);
    }

    public <R> Children notRightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + field + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children between(String field, R start, R end) {
        return this.between(true, field, start, end);
    }

    public <R> Children between(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + "(" + field + " BETWEEN ? AND ?)";
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
            String condition = this.getConditionPrefix() + "(" + field + " NOT BETWEEN ? AND ?)";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public Children and() {
        this.nextIsOr = false;
        return this.typedThis;
    }

    public <R> Children and(Consumer<Children> consumer) {
        return this.and(true, consumer);
    }

    public <R> Children and(boolean whether, Consumer<Children> consumer) {
        return this.whetherDo(whether, () -> {
            final Children instance = this.instance();
            consumer.accept(instance);
            String nestedCondition = instance.children();
            if (nestedCondition.isEmpty()) {
                return;
            }
            String condition = " AND " + nestedCondition;
            this.conditions.add(condition);
            this.whereParameters.addAll(instance.whereParameters);
        });
    }

    public Children or() {
        this.nextIsOr = true;
        return this.typedThis;
    }

    public <R> Children or(Consumer<Children> consumer) {
        return this.or(true, consumer);
    }

    public <R> Children or(boolean whether, Consumer<Children> consumer) {
        return this.whetherDo(whether, () -> {
            final Children instance = this.instance();
            consumer.accept(instance);
            String nestedCondition = instance.children();
            if (nestedCondition.isEmpty()) {
                return;
            }
            String condition = " OR " + nestedCondition;
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