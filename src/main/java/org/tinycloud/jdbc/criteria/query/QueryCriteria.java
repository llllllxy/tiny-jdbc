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
public class QueryCriteria extends AbstractCriteria<QueryCriteria> {

    public <R> QueryCriteria select(String... field) {
        if (!ObjectUtils.isEmpty(field)) {
            selectFields.addAll(Arrays.asList(field));
        }
        return this;
    }

    public QueryCriteria orderBy(String field, boolean desc) {
        String orderByString = field;
        if (desc) {
            orderByString += " DESC";
        }
        orderBy.add(orderByString);
        return this;
    }

    public QueryCriteria orderBy(String field) {
        String orderByString = field;
        orderBy.add(orderByString);
        return this;
    }
}