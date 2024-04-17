package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractCriteria;

import java.math.BigDecimal;


/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:36
 */
public class UpdateCriteria<T> extends AbstractCriteria<T, UpdateCriteria<T>> {

    public final <R> UpdateCriteria<T> set(String field, R value) {
        updateFields.add(field + "=?");
        parameters.add(value);
        return this;
    }

    public UpdateCriteria<T> setIncrement(String field, Number value) {
        updateFields.add(field + "=" + field + "+" + (value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    public UpdateCriteria<T> setDecrement(String field, Number value) {
        updateFields.add(field + "=" + field + "-" + (value instanceof BigDecimal ? ((BigDecimal) value).toPlainString() : value));
        return this;
    }

    @Override
    protected UpdateCriteria<T> instance() {
        return new UpdateCriteria<>();
    }
}
