package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-2024/4/14 22:32
 */
public final class Criterias {
    private Criterias() {
        // ignore
    }

    /**
     * 获取 QueryCriteria&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return QueryCriteria&lt;T&gt;
     */
    public static <T> QueryCriteria<T> query() {
        return new QueryCriteria<>();
    }

    /**
     * 获取 LambdaQueryCriteria&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return LambdaQueryCriteria&lt;T&gt;
     */
    public static <T> LambdaQueryCriteria<T> lambdaQuery() {
        return new LambdaQueryCriteria<>();
    }

    /**
     * 获取 UpdateCriteria&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return UpdateCriteria&lt;T&gt;
     */
    public static <T> UpdateCriteria<T> update() {
        return new UpdateCriteria<>();
    }

    /**
     * 获取 LambdaUpdateCriteria&lt;T&gt;
     *
     * @param <T> 实体类泛型
     * @return LambdaUpdateCriteria&lt;T&gt;
     */
    public static <T> LambdaUpdateCriteria<T> lambdaUpdate() {
        return new LambdaUpdateCriteria<>();
    }
}
