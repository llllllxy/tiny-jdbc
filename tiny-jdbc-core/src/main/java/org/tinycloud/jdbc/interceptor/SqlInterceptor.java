package org.tinycloud.jdbc.interceptor;

/**
 * SQL 环绕拦截器，可在执行前改写请求，并在执行后观察结果或处理异常。
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
@FunctionalInterface
public interface SqlInterceptor {

    /**
     * 拦截 SQL 请求并决定是否继续执行后续链路。
     *
     * @param request 当前 SQL 请求
     * @param chain   后续 SQL 拦截器链
     * @param <R>     执行结果类型
     * @return SQL 执行结果
     */
    <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain);
}
