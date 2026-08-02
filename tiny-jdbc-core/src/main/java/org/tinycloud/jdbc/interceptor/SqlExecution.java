package org.tinycloud.jdbc.interceptor;

/**
 * SQL 请求的最终执行器。
 *
 * @param <R> 执行结果类型
 */
@FunctionalInterface
public interface SqlExecution<R> {

    /**
     * 使用当前请求中的 SQL 和参数执行数据库操作。
     *
     * @param request 当前 SQL 请求
     * @return 数据库执行结果
     */
    R execute(SqlRequest<R> request);
}
