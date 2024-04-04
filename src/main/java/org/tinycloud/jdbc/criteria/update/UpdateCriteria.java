package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractCriteria;


/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:36
 */
public class UpdateCriteria extends AbstractCriteria<UpdateCriteria> {

    public <R> UpdateCriteria set(String field, R value) {
        updateFields.add(field + "=?");
        parameters.add(value);
        return this;
    }
}
