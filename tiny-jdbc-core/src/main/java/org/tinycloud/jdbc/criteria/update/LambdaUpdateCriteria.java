package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractLambdaCriteria;
import org.tinycloud.jdbc.criteria.TypeFunction;

import java.math.BigDecimal;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:36
 */
public class LambdaUpdateCriteria<T> extends AbstractLambdaCriteria<T, LambdaUpdateCriteria<T>> {

    public <R> LambdaUpdateCriteria<T> set(TypeFunction<T, ?> field, R value) {
        String columnName = getColumnName(field);
        updateFields.add(columnName + "=?");
        parameters.add(value);
        return this;
    }

    public LambdaUpdateCriteria<T> setIncrement(TypeFunction<T, ?> field, Number value) {
        String columnName = getColumnName(field);
        updateFields.add(columnName + "=" + columnName + "+" + (value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    public LambdaUpdateCriteria<T> setDecrement(TypeFunction<T, ?> field, Number value) {
        String columnName = getColumnName(field);
        updateFields.add(columnName + "=" + columnName + "-" + (value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    @Override
    protected LambdaUpdateCriteria<T> instance() {
        return new LambdaUpdateCriteria<>();
    }
}
