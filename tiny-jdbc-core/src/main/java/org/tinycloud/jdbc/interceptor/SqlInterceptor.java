package org.tinycloud.jdbc.interceptor;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 用于在构建sql、参数数组钱执行相应逻辑，
 * 可用于添加统一的条件、强制更新时间戳、记录sql等,
 *
 * @author liuxingyu01
 * @since 2025-12-10 14:20
 */
public interface SqlInterceptor {
    /**
     * 在真正构建sql和参数之前执行，该方法会影响最终的sql和参数
     *
     * @param invocation   SqlInvocation
     * @param jdbcTemplate JdbcTemplate
     **/
    default void before(SqlInvocation invocation, JdbcTemplate jdbcTemplate) {
        // do nothing
    }

    /**
     * SQL执行完成后方法（主要用于对返回值修改）
     *
     * @param result       执行结果
     * @param invocation   SqlInvocation
     * @param jdbcTemplate JdbcTemplate
     **/
    default Object after(Object result, SqlInvocation invocation, JdbcTemplate jdbcTemplate) {
        // do nothing
        return result;
    }
}
