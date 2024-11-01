package org.tinycloud.jdbc.criteria.query;

import org.springframework.util.ObjectUtils;
import org.tinycloud.jdbc.criteria.AbstractCriteria;

import java.util.Arrays;

/**
 * <p>
 * 查询操作-条件构造器
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class QueryCriteria<T> extends AbstractCriteria<T, QueryCriteria<T>> {

    public final QueryCriteria<T> select(String... field) {
        if (!ObjectUtils.isEmpty(field)) {
            selectFields.addAll(Arrays.asList(field));
        }
        return this;
    }

    public final QueryCriteria<T> orderBy(String field, boolean isDesc) {
        String orderByString = field;
        if (isDesc) {
            orderByString += " DESC";
        }
        orderBys.add(orderByString);
        return this;
    }

    public final QueryCriteria<T> orderBy(String field) {
        String orderByString = field;
        orderBys.add(orderByString);
        return this;
    }

    public final QueryCriteria<T> last(String lastSql) {
        lastSqls.clear();
        lastSqls.add(lastSql);
        return this;
    }

    @Override
    protected QueryCriteria<T> instance() {
        return new QueryCriteria<T>();
    }
}