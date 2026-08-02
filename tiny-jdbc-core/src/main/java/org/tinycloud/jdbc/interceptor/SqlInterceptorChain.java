package org.tinycloud.jdbc.interceptor;

/**
 * SQL 拦截器链，负责将请求传递给下一个拦截器或最终执行器。
 *
 * @param <R> 执行结果类型
 */
public interface SqlInterceptorChain<R> {

    /**
     * 继续执行链路中的下一个节点。
     *
     * @param request 当前 SQL 请求
     * @return 数据库执行结果
     */
    R proceed(SqlRequest<R> request);
}
