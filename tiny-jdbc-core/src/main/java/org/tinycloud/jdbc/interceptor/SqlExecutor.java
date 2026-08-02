package org.tinycloud.jdbc.interceptor;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SQL 执行器，负责按顺序驱动 SQL 拦截器链和最终数据库操作。
 */
public class SqlExecutor {
    private final List<SqlInterceptor> interceptors;

    /**
     * 创建 SQL 执行器并按 Spring 排序规则固定拦截器顺序。
     *
     * @param interceptors SQL 拦截器集合
     */
    public SqlExecutor(Collection<SqlInterceptor> interceptors) {
        this.interceptors = new ArrayList<>();
        if (interceptors != null) {
            this.interceptors.addAll(interceptors);
        }
        AnnotationAwareOrderComparator.sort(this.interceptors);
    }

    /**
     * 执行 SQL 请求，使其依次经过全部拦截器。
     *
     * @param request   SQL 请求
     * @param execution 最终数据库执行器
     * @param <R>       执行结果类型
     * @return 数据库执行结果
     */
    public <R> R execute(SqlRequest<R> request, SqlExecution<R> execution) {
        return new DefaultSqlInterceptorChain<>(execution).proceed(request);
    }

    /**
     * 默认拦截器链实现，使用索引避免为每个节点重复创建链对象。
     *
     * @param <R> 执行结果类型
     */
    private final class DefaultSqlInterceptorChain<R> implements SqlInterceptorChain<R> {
        private final SqlExecution<R> execution;
        private int index;

        /**
         * 创建默认 SQL 拦截器链。
         *
         * @param execution 最终数据库执行器
         */
        private DefaultSqlInterceptorChain(SqlExecution<R> execution) {
            this.execution = execution;
        }

        /**
         * 执行下一个拦截器，链尾执行最终数据库操作。
         *
         * @param request 当前 SQL 请求
         * @return 数据库执行结果
         */
        @Override
        public R proceed(SqlRequest<R> request) {
            if (this.index < SqlExecutor.this.interceptors.size()) {
                SqlInterceptor interceptor = SqlExecutor.this.interceptors.get(this.index++);
                return interceptor.intercept(request, this);
            }
            return this.execution.execute(request);
        }
    }
}
