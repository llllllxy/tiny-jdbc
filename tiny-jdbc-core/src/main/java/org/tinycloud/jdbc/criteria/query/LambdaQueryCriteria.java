package org.tinycloud.jdbc.criteria.query;

import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.criteria.AbstractLambdaCriteria;
import org.tinycloud.jdbc.criteria.TypeFunction;

/**
 * <p>
 * 查询操作-条件构造器（Lambda版）
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class LambdaQueryCriteria<T> extends AbstractLambdaCriteria<T, LambdaQueryCriteria<T>> {

    @SafeVarargs
    public final LambdaQueryCriteria<T> select(TypeFunction<T, ?>... field) {
        if (!ObjectUtils.isEmpty(field)) {
            for (TypeFunction<T, ?> f : field) {
                String columnName = getColumnName(f);
                selectFields.add(columnName);
            }
        }
        return this;
    }

    public LambdaQueryCriteria<T> orderBy(TypeFunction<T, ?> field, boolean isDesc) {
        return orderBy(true, field, isDesc);
    }

    public LambdaQueryCriteria<T> orderBy(boolean whether, TypeFunction<T, ?> field, boolean isDesc) {
        if (whether) {
            String columnName = getColumnName(field);
            if (isDesc) {
                columnName += " DESC";
            }
            orderBys.add(columnName);
        }
        return this;
    }

    public LambdaQueryCriteria<T> orderBy(TypeFunction<T, ?> field) {
        return orderBy(true, field);
    }

    public LambdaQueryCriteria<T> orderBy(boolean whether, TypeFunction<T, ?> field) {
        if (whether) {
            String columnName = getColumnName(field);
            orderBys.add(columnName);
        }
        return this;
    }

    public final LambdaQueryCriteria<T> last(String lastSql) {
        lastSqls.clear();
        lastSqls.add(lastSql);
        return this;
    }

    @Override
    protected LambdaQueryCriteria<T> instance() {
        return new LambdaQueryCriteria<>();
    }
}
