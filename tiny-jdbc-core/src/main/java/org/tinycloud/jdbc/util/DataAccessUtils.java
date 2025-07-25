package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.Collection;

/**
 * <p>
 *     数据访问和验证工具
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-03-31 22:43
 */
public class DataAccessUtils {
    public DataAccessUtils() {
    }

    /**
     * 获取集合中的唯一元素
     *
     * @param results 结果集合
     * @param <T>     元素类型
     * @return 唯一元素或null
     * @throws TinyJdbcException 集合中存在多个元素时抛出
     */
    public static <T> T singleResult(Collection<T> results) throws TinyJdbcException {
        if (results == null || results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new TinyJdbcException("Expected one result (or null) to be returned , but found " + results.size() + " records");
        } else {
            return results.iterator().next();
        }
    }

    /**
     * 获取集合中的唯一元素（非空）
     *
     * @param results 结果集合
     * @param <T>     元素类型
     * @return 唯一元素
     * @throws TinyJdbcException 集合中不存在或存在多个元素时抛出
     */
    public static <T> T requiredSingleResult(Collection<T> results) throws TinyJdbcException {
        if (results == null || results.isEmpty()) {
            throw new TinyJdbcException("Expected one result to be returned , but not found");
        } else if (results.size() > 1) {
            throw new TinyJdbcException("Expected one result to be returned , but found " + results.size() + " records");
        } else {
            return results.iterator().next();
        }
    }
}
