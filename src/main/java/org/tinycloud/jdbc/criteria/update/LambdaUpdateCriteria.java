package org.tinycloud.jdbc.criteria.update;

import org.tinycloud.jdbc.criteria.AbstractLambdaCriteria;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-03 14:36
 */
public class LambdaUpdateCriteria extends AbstractLambdaCriteria<LambdaUpdateCriteria> {

    public <T, R> LambdaUpdateCriteria set(TypeFunction<T, R> field, R value) {
        String columnName = getColumnName(field);
        updateFields.add(columnName + "=?");
        parameters.add(value);
        return this;
    }

    @Override
    protected LambdaUpdateCriteria instance() {
        return new LambdaUpdateCriteria();
    }
}
