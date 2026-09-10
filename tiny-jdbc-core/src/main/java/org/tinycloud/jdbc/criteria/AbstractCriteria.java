package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.util.SqlIdentifierUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " < ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children lte(String field, R value) {
        return this.lte(true, field, value);
    }

    public <R> Children lte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " <= ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gt(String field, R value) {
        return this.gt(true, field, value);
    }

    public <R> Children gt(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " > ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children gte(String field, R value) {
        return this.gte(true, field, value);
    }

    public <R> Children gte(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " >= ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children eq(String field, R value) {
        return this.eq(true, field, value);
    }

    public <R> Children eq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " = ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children notEq(String field, R value) {
        return this.notEq(true, field, value);
    }

    public <R> Children notEq(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " <> ?";
            this.conditions.add(condition);
            this.whereParameters.add(value);
        });
    }

    public <R> Children isNull(String field) {
        return this.isNull(true, field);
    }

    public <R> Children isNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " IS NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children isNotNull(String field) {
        return this.isNotNull(true, field);
    }

    public <R> Children isNotNull(boolean whether, String field) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " IS NOT NULL";
            this.conditions.add(condition);
        });
    }

    public <R> Children in(String field, Collection<R> values) {
        return this.in(true, field, values);
    }

    public <R> Children in(boolean whether, String field, Collection<R> values) {
        return this.whetherDo(whether, () -> {
            this.validateInValues(field, values);
            StringBuilder condition = new StringBuilder();
            condition.append(getConditionPrefix())
                    .append(this.checkedColumnRef(field))
                    .append(" IN (")
                    .append(IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    /**
     * 字段 IN (v0, v1, ...)，可变参数版（与 Collection 版 {@link #in(boolean, String, Collection)} 同名共存）。
     * <p>注意：请勿传入裸 {@code null}，否则 Java 重载会产生歧义；空值请传 {@code Collections.emptyList()}。</p>
     */
    public <R> Children in(String field, Object... values) {
        return this.in(true, field, values);
    }

    public <R> Children in(boolean whether, String field, Object... values) {
        return this.in(whether, field, Arrays.asList(values));
    }

    public <R> Children notIn(String field, Collection<R> values) {
        return this.notIn(true, field, values);
    }

    public <R> Children notIn(boolean whether, String field, Collection<R> values) {
        return this.whetherDo(whether, () -> {
            this.validateInValues(field, values);
            StringBuilder condition = new StringBuilder();
            condition.append(getConditionPrefix())
                    .append(this.checkedColumnRef(field))
                    .append(" NOT IN (")
                    .append(IntStream.range(0, values.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            this.conditions.add(condition.toString());
            this.whereParameters.addAll(values);
        });
    }

    /**
     * 字段 NOT IN (v0, v1, ...)，可变参数版（与 Collection 版 {@link #notIn(boolean, String, Collection)} 同名共存）。
     * <p>注意：请勿传入裸 {@code null}，否则 Java 重载会产生歧义；空值请传 {@code Collections.emptyList()}。</p>
     */
    public <R> Children notIn(String field, Object... values) {
        return this.notIn(true, field, values);
    }

    public <R> Children notIn(boolean whether, String field, Object... values) {
        return this.notIn(whether, field, Arrays.asList(values));
    }

    public <R> Children like(String field, R value) {
        return this.like(true, field, value);
    }

    public <R> Children like(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children notLike(String field, R value) {
        return this.notLike(true, field, value);
    }

    public <R> Children notLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value + "%");
        });
    }

    public <R> Children leftLike(String field, R value) {
        return this.leftLike(true, field, value);
    }

    public <R> Children leftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children notLeftLike(String field, R value) {
        return this.notLeftLike(true, field, value);
    }

    public <R> Children notLeftLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add("%" + value);
        });
    }

    public <R> Children rightLike(String field, R value) {
        return this.rightLike(true, field, value);
    }

    public <R> Children rightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children notRightLike(String field, R value) {
        return this.notRightLike(true, field, value);
    }

    public <R> Children notRightLike(boolean whether, String field, R value) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + this.checkedColumnRef(field) + " NOT LIKE ?";
            this.conditions.add(condition);
            this.whereParameters.add(value + "%");
        });
    }

    public <R> Children between(String field, R start, R end) {
        return this.between(true, field, start, end);
    }

    public <R> Children between(boolean whether, String field, R start, R end) {
        return this.whetherDo(whether, () -> {
            String condition = this.getConditionPrefix() + "(" + this.checkedColumnRef(field) + " BETWEEN ? AND ?)";
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
            String condition = this.getConditionPrefix() + "(" + this.checkedColumnRef(field) + " NOT BETWEEN ? AND ?)";
            this.conditions.add(condition);
            this.whereParameters.add(start);
            this.whereParameters.add(end);
        });
    }

    /**
     * 追加 {@code EXISTS} 子查询条件（如 {@code "select 1 from t2 where t2.id = t1.id"}）。
     * <p>子查询整体以小括号包裹，参数按出现顺序绑定到 {@code ?}；默认做尾部片段安全校验。</p>
     */
    public Children exists(String existsSql) {
        return this.exists(true, existsSql, (Object[]) null);
    }

    /**
     * 追加 {@code EXISTS} 子查询条件。
     *
     * @param whether  是否添加该条件
     * @param existsSql 子查询 SQL
     * @return 当前条件构造器
     */
    public Children exists(boolean whether, String existsSql) {
        return this.exists(whether, existsSql, (Object[]) null);
    }

    /**
     * 追加 {@code EXISTS} 子查询条件。
     *
     * @param existsSql 子查询 SQL（可含 {@code ?} 占位符）
     * @param params    绑定到 {@code ?} 的参数
     * @return 当前条件构造器
     */
    public Children exists(String existsSql, Object... params) {
        return this.exists(true, existsSql, params);
    }

    /**
     * 追加 {@code EXISTS} 子查询条件。
     */
    public Children exists(boolean whether, String existsSql, Object... params) {
        return this.whetherDo(whether, () -> {
            SqlIdentifierUtils.checkTailSql(existsSql);
            String condition = this.getConditionPrefix() + "EXISTS (" + existsSql + ")";
            this.conditions.add(condition);
            if (params != null) {
                Collections.addAll(this.whereParameters, params);
            }
        });
    }

    /**
     * 追加 {@code NOT EXISTS} 子查询条件（如 {@code "select 1 from t2 where t2.id = t1.id"}）。
     */
    public Children notExists(String existsSql) {
        return this.notExists(true, existsSql, (Object[]) null);
    }

    /**
     * 追加 {@code NOT EXISTS} 子查询条件。
     *
     * @param whether   是否添加该条件
     * @param existsSql 子查询 SQL
     * @return 当前条件构造器
     */
    public Children notExists(boolean whether, String existsSql) {
        return this.notExists(whether, existsSql, (Object[]) null);
    }

    /**
     * 追加 {@code NOT EXISTS} 子查询条件。
     *
     * @param existsSql 子查询 SQL（可含 {@code ?} 占位符）
     * @param params    绑定到 {@code ?} 的参数
     * @return 当前条件构造器
     */
    public Children notExists(String existsSql, Object... params) {
        return this.notExists(true, existsSql, params);
    }

    /**
     * 追加 {@code NOT EXISTS} 子查询条件。
     */
    public Children notExists(boolean whether, String existsSql, Object... params) {
        return this.whetherDo(whether, () -> {
            SqlIdentifierUtils.checkTailSql(existsSql);
            String condition = this.getConditionPrefix() + "NOT EXISTS (" + existsSql + ")";
            this.conditions.add(condition);
            if (params != null) {
                Collections.addAll(this.whereParameters, params);
            }
        });
    }

    /**
     * 追加一段<b>受信任</b>的原始 SQL 条件片段，支持 {@code ?} 参数绑定。
     * <p>用于字段间比较、函数条件等无法用标准方法表达的场景
     * （如 {@code "date(create_time) = ?"}、{@code "a = b"}）——类似于 MyBatis-Plus 的 {@code apply}。
     * 默认做尾部片段安全校验，表达式中的字符串值请用 {@code ?} 参数绑定。</p>
     */
    public Children apply(String applySql) {
        return this.apply(true, applySql, (Object[]) null);
    }

    /**
     * 追加一段<b>受信任</b>的原始 SQL 条件片段。
     *
     * @param whether  是否添加该条件
     * @param applySql 原 SQL 条件片段
     * @return 当前条件构造器
     */
    public Children apply(boolean whether, String applySql) {
        return this.apply(whether, applySql, (Object[]) null);
    }

    /**
     * 追加一段<b>受信任</b>的原始 SQL 条件片段（可含 {@code ?} 占位符）。
     *
     * @param applySql 原 SQL 条件片段
     * @param params   绑定到 {@code ?} 的参数
     * @return 当前条件构造器
     */
    public Children apply(String applySql, Object... params) {
        return this.apply(true, applySql, params);
    }

    /**
     * 追加一段<b>受信任</b>的原始 SQL 条件片段。
     */
    public Children apply(boolean whether, String applySql, Object... params) {
        return this.whetherDo(whether, () -> {
            SqlIdentifierUtils.checkTailSql(applySql);
            String condition = this.getConditionPrefix() + applySql;
            this.conditions.add(condition);
            if (params != null) {
                Collections.addAll(this.whereParameters, params);
            }
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
