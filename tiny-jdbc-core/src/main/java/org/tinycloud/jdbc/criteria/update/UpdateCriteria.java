package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractCriteria;


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

    @Override
    protected UpdateCriteria<T> instance() {
        return new UpdateCriteria<>();
    }
}
