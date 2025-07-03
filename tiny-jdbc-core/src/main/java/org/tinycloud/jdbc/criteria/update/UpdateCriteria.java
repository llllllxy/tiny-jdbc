package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractCriteria;

import java.math.BigDecimal;


/**
 * <p>
 * 更新条件构造器
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:36
 */
public class UpdateCriteria<T> extends AbstractCriteria<T, UpdateCriteria<T>> {

    /**
     * 根据条件设置更新字段及其对应的值。
     * 若条件为真，则将指定字段和对应的值添加到更新字段列表和参数列表中。
     *
     * @param whether 一个布尔值，决定是否执行字段和值的添加操作。若为 true 则添加，为 false 则忽略。
     * @param field   要更新的数据库字段名。
     * @param value   要设置给该字段的新值，泛型类型，可接受任意类型的值。
     * @param <R>     值的泛型类型。
     * @return 当前的 UpdateCriteria 实例，用于链式调用。
     */
    public final <R> UpdateCriteria<T> set(boolean whether, String field, R value) {
        if (whether) {
            this.updateFields.add(field + " = ?");
            this.updateParameters.add(value);
        }
        return this;
    }

    /**
     * 设置更新字段及其对应的值。
     * 该方法会调用 {@link #set(boolean, String, Object)} 方法，并将条件参数设置为 true，
     * 确保指定的字段和值一定会被添加到更新字段列表和参数列表中。
     *
     * @param field 要更新的数据库字段名。
     * @param value 要设置给该字段的新值，泛型类型，可接受任意类型的值。
     * @param <R>   值的泛型类型。
     * @return 当前的 UpdateCriteria 实例，用于链式调用。
     */
    public final <R> UpdateCriteria<T> set(String field, R value) {
        return this.set(true, field, value);
    }

    /**
     * 设置指定字段的值按给定数值进行递增操作。
     * 该方法会生成一条 SQL 语句片段，将指定字段的值加上给定的数值，
     * 并将生成的 SQL 片段添加到更新字段列表中，用于后续的数据库更新操作。
     * 对于 BigDecimal 类型的数值，会使用其无科学计数法的字符串表示形式。
     *
     * @param field 要进行递增操作的数据库字段名
     * @param value 用于递增的数值，支持 Number 类型及其子类
     * @return 当前的 UpdateCriteria 实例，用于链式调用
     */
    public UpdateCriteria<T> setIncrement(String field, Number value) {
        this.updateFields.add(String.format("%s=%s + %s", field, field, value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    /**
     * 设置指定字段的值按给定数值进行递减操作。
     * 该方法会生成一条 SQL 语句片段，将指定字段的值减去给定的数值，
     * 并将生成的 SQL 片段添加到更新字段列表中，用于后续的数据库更新操作。
     * 对于 BigDecimal 类型的数值，会使用其无科学计数法的字符串表示形式。
     *
     * @param field 要进行递减操作的数据库字段名
     * @param value 用于递减的数值，支持 Number 类型及其子类
     * @return 当前的 UpdateCriteria 实例，用于链式调用
     */
    public UpdateCriteria<T> setDecrement(String field, Number value) {
        this.updateFields.add(String.format("%s=%s - %s", field, field, value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    /**
     * 重写父类的 instance 方法，用于创建当前类的一个新实例。
     * 该方法通常在链式调用或构建新的条件构造器时使用，
     * 通过返回一个新的 UpdateCriteria 实例，确保每次操作都可以基于新的上下文进行。
     *
     * @return 一个新的 UpdateCriteria 实例
     */
    @Override
    protected UpdateCriteria<T> instance() {
        return new UpdateCriteria<>();
    }
}
