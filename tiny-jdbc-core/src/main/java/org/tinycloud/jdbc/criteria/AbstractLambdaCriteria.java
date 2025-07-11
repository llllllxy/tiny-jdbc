package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * 查询条件构造器-抽象类（lambda版）
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:40
 */
@SuppressWarnings({"unchecked"})
public abstract class AbstractLambdaCriteria<T, Children extends AbstractLambdaCriteria<T, Children>> extends Criteria<T> {

    /**
     * 占位符
     */
    protected final Children typedThis = (Children) this;

    public <R> Children lt(TypeFunction<T, ?> field, R value) {
        return this.lt(true, field, value);
    }

    public <R> Children lt(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " < " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orLt(TypeFunction<T, ?> field, R value) {
        return this.orLt(true, field, value);
    }

    public <R> Children orLt(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " < " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children lte(TypeFunction<T, ?> field, R value) {
        return this.lte(true, field, value);
    }

    public <R> Children lte(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " <= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orLte(TypeFunction<T, ?> field, R value) {
        return this.orLte(true, field, value);
    }

    public <R> Children orLte(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " <= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gt(TypeFunction<T, ?> field, R value) {
        return this.gt(true, field, value);
    }

    public <R> Children gt(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " > " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orGt(TypeFunction<T, ?> field, R value) {
        return this.orGt(true, field, value);
    }

    public <R> Children orGt(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " > " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gte(TypeFunction<T, ?> field, R value) {
        return this.gte(true, field, value);
    }

    public <R> Children gte(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " >= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orGte(TypeFunction<T, ?> field, R value) {
        return this.orGte(true, field, value);
    }

    public <R> Children orGte(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " >= " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children eq(TypeFunction<T, ?> field, R value) {
        return this.eq(true, field, value);
    }

    public <R> Children eq(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " = " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orEq(TypeFunction<T, ?> field, R value) {
        return this.orEq(true, field, value);
    }

    public <R> Children orEq(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " = " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children notEq(TypeFunction<T, ?> field, R value) {
        return this.notEq(true, field, value);
    }

    public <R> Children notEq(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " <> " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children orNotEq(TypeFunction<T, ?> field, R value) {
        return this.orNotEq(true, field, value);
    }

    public <R> Children orNotEq(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " <> " + "?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children isNull(TypeFunction<T, ?> field) {
        return this.isNull(true, field);
    }

    public <R> Children isNull(boolean whether, TypeFunction<T, ?> field) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children orIsNull(TypeFunction<T, ?> field) {
        return this.orIsNull(true, field);
    }

    public <R> Children orIsNull(boolean whether, TypeFunction<T, ?> field) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children isNotNull(TypeFunction<T, ?> field) {
        return this.isNotNull(true, field);
    }

    public <R> Children isNotNull(boolean whether, TypeFunction<T, ?> field) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children orIsNotNull(TypeFunction<T, ?> field) {
        return this.orIsNotNull(true, field);
    }

    public <R> Children orIsNotNull(boolean whether, TypeFunction<T, ?> field) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children in(TypeFunction<T, ?> field, List<R> values) {
        return this.in(true, field, values);
    }

    public <R> Children in(boolean whether, TypeFunction<T, ?> field, List<R> values) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(columnName).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children orIn(TypeFunction<T, ?> field, List<R> values) {
        return this.orIn(true, field, values);
    }

    public <R> Children orIn(boolean whether, TypeFunction<T, ?> field, List<R> values) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(columnName).append(" IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children notIn(TypeFunction<T, ?> field, List<R> values) {
        return this.notIn(true, field, values);
    }

    public <R> Children notIn(boolean whether, TypeFunction<T, ?> field, List<R> values) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            StringBuilder condition = new StringBuilder();
            condition.append(" AND ").append(columnName).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children orNotIn(TypeFunction<T, ?> field, List<R> values) {
        return this.orNotIn(true, field, values);
    }

    public <R> Children orNotIn(boolean whether, TypeFunction<T, ?> field, List<R> values) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            StringBuilder condition = new StringBuilder();
            condition.append(" OR ").append(columnName).append(" NOT IN (");
            condition.append(String.join(", ", Collections.nCopies(values.size(), "?")));
            condition.append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    public <R> Children like(TypeFunction<T, ?> field, R value) {
        return this.like(true, field, value);
    }

    public <R> Children like(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children orLike(TypeFunction<T, ?> field, R value) {
        return this.orLike(true, field, value);
    }

    public <R> Children orLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children notLike(TypeFunction<T, ?> field, R value) {
        return this.notLike(true, field, value);
    }

    public <R> Children notLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children orNotLike(TypeFunction<T, ?> field, R value) {
        return this.orNotLike(true, field, value);
    }

    public <R> Children orNotLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children leftLike(TypeFunction<T, ?> field, R value) {
        return this.leftLike(true, field, value);
    }

    public <R> Children leftLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children orLeftLike(TypeFunction<T, ?> field, R value) {
        return this.orLeftLike(true, field, value);
    }

    public <R> Children orLeftLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children notLeftLike(TypeFunction<T, ?> field, R value) {
        return this.notLeftLike(true, field, value);
    }

    public <R> Children notLeftLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children orNotLeftLike(TypeFunction<T, ?> field, R value) {
        return this.orNotLeftLike(true, field, value);
    }

    public <R> Children orNotLeftLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children rightLike(TypeFunction<T, ?> field, R value) {
        return this.rightLike(true, field, value);
    }

    public <R> Children rightLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children orRightLike(TypeFunction<T, ?> field, R value) {
        return this.orRightLike(true, field, value);
    }

    public <R> Children orRightLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children notRightLike(TypeFunction<T, ?> field, R value) {
        return this.notRightLike(true, field, value);
    }

    public <R> Children notRightLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children orNotRightLike(TypeFunction<T, ?> field, R value) {
        return this.orNotRightLike(true, field, value);
    }

    public <R> Children orNotRightLike(boolean whether, TypeFunction<T, ?> field, R value) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + columnName + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children between(TypeFunction<T, ?> field, R start, R end) {
        return this.between(true, field, start, end);
    }

    public <R> Children between(boolean whether, TypeFunction<T, ?> field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + "(" + columnName + " BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children orBetween(TypeFunction<T, ?> field, R start, R end) {
        return this.orBetween(true, field, start, end);
    }

    public <R> Children orBetween(boolean whether, TypeFunction<T, ?> field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + "(" + columnName + " BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children notBetween(TypeFunction<T, ?> field, R start, R end) {
        return this.notBetween(true, field, start, end);
    }

    public <R> Children notBetween(boolean whether, TypeFunction<T, ?> field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " AND " + "(" + columnName + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public <R> Children orNotBetween(TypeFunction<T, ?> field, R start, R end) {
        return this.orNotBetween(true, field, start, end);
    }

    public <R> Children orNotBetween(boolean whether, TypeFunction<T, ?> field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String columnName = this.getColumnName(field);
            String condition = " OR " + "(" + columnName + " NOT BETWEEN " + "?" + " AND " + "?" + ")";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    public Children and(Consumer<Children> consumer) {
        return this.and(true, consumer);
    }

    public Children and(boolean whether, Consumer<Children> consumer) {
        return this.whetherDo(whether, () -> {
            final Children instance = this.instance();
            consumer.accept(instance);
            String condition = " AND " + instance.children();
            this.conditions.add(condition);
            this.whereParameters.addAll(instance.whereParameters);
        });
    }

    public Children or(Consumer<Children> consumer) {
        return this.or(true, consumer);
    }

    public Children or(boolean whether, Consumer<Children> consumer) {
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

    public String getColumnName(TypeFunction<T, ?> field) {
        return LambdaUtils.getLambdaColumnName(field);
    }

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
