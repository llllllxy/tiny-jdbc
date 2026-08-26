package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.interceptor.SqlExecutor;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.interceptor.SqlInterceptorChain;
import org.tinycloud.jdbc.interceptor.SqlRequest;
import org.tinycloud.jdbc.interceptor.SqlType;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 使用 main 方法验证 SQL 执行器的拦截链行为。
 */
public class SqlExecutorVerifyMain {

    /**
     * 验证 SQL 改写、嵌套顺序、异常路径和批量请求参数隔离。
     *
     * @param args 命令行参数
     */
    @Test public void testAll() {
        verifyRewriteAndChainOrder();
        verifyExceptionUnwindsChain();
        verifyBatchRequestCopiesArguments();
        verifyAbstractSqlSupportUsesInterceptorChain();
        System.out.println("SqlExecutorVerifyMain passed.");
    }

    /**
     * 验证拦截器改写的 SQL 与参数会传递到最终执行器，且链路按嵌套顺序退出。
     */
    private static void verifyRewriteAndChainOrder() {
        List<String> events = new ArrayList<>();
        SqlInterceptor outer = new SqlInterceptor() {
            @Override
            public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
                events.add("outer-before");
                try {
                    return chain.proceed(request);
                } finally {
                    events.add("outer-after");
                }
            }
        };
        SqlInterceptor rewrite = new SqlInterceptor() {
            @Override
            public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
                events.add("rewrite-before");
                request.setStatement("SELECT ?", new Object[]{2L});
                try {
                    return chain.proceed(request);
                } finally {
                    events.add("rewrite-after");
                }
            }
        };
        SqlExecutor executor = new SqlExecutor(Arrays.asList(outer, rewrite));
        String result = executor.execute(new SqlRequest<>("SELECT ?", new Object[]{1L}, SqlType.QUERY), request -> {
            events.add("execute");
            assertEquals("SELECT ?", request.getSql(), "rewritten SQL mismatch");
            assertEquals(2L, request.getArgs()[0], "rewritten parameter mismatch");
            return "ok";
        });

        assertEquals("ok", result, "execution result mismatch");
        assertEquals(Arrays.asList("outer-before", "rewrite-before", "execute", "rewrite-after", "outer-after"), events,
                "interceptor chain order mismatch");
    }

    /**
     * 验证最终执行失败时，已进入拦截器的 finally 块仍会执行。
     */
    private static void verifyExceptionUnwindsChain() {
        List<String> events = new ArrayList<>();
        SqlInterceptor interceptor = new SqlInterceptor() {
            @Override
            public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
                events.add("before");
                try {
                    return chain.proceed(request);
                } finally {
                    events.add("after");
                }
            }
        };
        SqlExecutor executor = new SqlExecutor(Collections.singletonList(interceptor));
        try {
            executor.execute(new SqlRequest<Void>("SELECT 1", null, SqlType.QUERY), request -> {
                throw new IllegalStateException("expected");
            });
        } catch (IllegalStateException e) {
            assertEquals("expected", e.getMessage(), "unexpected execution exception");
            assertEquals(Arrays.asList("before", "after"), events, "exception chain order mismatch");
            return;
        }
        throw new IllegalStateException("execution exception expected");
    }

    /**
     * 验证批量请求会复制调用方参数，避免后续修改影响待执行参数。
     */
    private static void verifyBatchRequestCopiesArguments() {
        List<Object[]> batchArgs = new ArrayList<>();
        batchArgs.add(new Object[]{1L});
        SqlRequest<int[]> request = SqlRequest.batch("INSERT INTO demo(id) VALUES (?)", batchArgs);
        batchArgs.get(0)[0] = 2L;

        assertEquals(1L, request.getBatchArgs().get(0)[0], "batch request should copy arguments");
    }

    /**
     * 验证 AbstractSqlSupport 会通过统一执行器执行拦截器改写后的请求。
     */
    private static void verifyAbstractSqlSupportUsesInterceptorChain() {
        SqlInterceptor rewrite = new SqlInterceptor() {
            @Override
            public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
                request.setStatement("SELECT ?", new Object[]{2L});
                return chain.proceed(request);
            }
        };
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        TestSqlSupport support = new TestSqlSupport(jdbcTemplate, Collections.singletonList(rewrite));
        Long result = support.selectOneObject("SELECT ?", Long.class, 1L);

        assertEquals(2L, result, "query result mismatch");
        assertEquals("SELECT ?", jdbcTemplate.getSql(), "AbstractSqlSupport should use rewritten SQL");
        assertEquals(2L, jdbcTemplate.getArgs()[0], "AbstractSqlSupport should use rewritten parameters");
    }

    /**
     * 断言两个对象相等。
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param message  断言失败提示
     */
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    /**
     * 用于记录最终 JDBC 调用参数的测试 JdbcTemplate。
     */
    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] args;

        /**
         * 记录单对象查询的 SQL 与参数，并返回固定结果。
         *
         * @param sql          查询 SQL
         * @param requiredType 返回类型
         * @param args         查询参数
         * @param <T>          返回类型
         * @return 固定查询结果
         */
        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            this.sql = sql;
            this.args = args;
            return requiredType.cast(2L);
        }

        /**
         * 获取最终执行的 SQL。
         *
         * @return 最终执行的 SQL
         */
        private String getSql() {
            return this.sql;
        }

        /**
         * 获取最终执行的 SQL 参数。
         *
         * @return 最终执行的 SQL 参数
         */
        private Object[] getArgs() {
            return this.args;
        }
    }

    /**
     * 提供 AbstractSqlSupport 运行所需依赖的测试实现。
     */
    private static class TestSqlSupport extends AbstractSqlSupport<VerifyDemoEntity, Long> {
        private final JdbcTemplate jdbcTemplate;
        private final List<SqlInterceptor> interceptors;
        private final TinyJdbcRuntime tinyJdbcRuntime;

        /**
         * 创建测试 SQL 支持对象。
         *
         * @param jdbcTemplate JDBC 模板
         * @param interceptors SQL 拦截器列表
         */
        private TestSqlSupport(JdbcTemplate jdbcTemplate, List<SqlInterceptor> interceptors) {
            this.jdbcTemplate = jdbcTemplate;
            this.interceptors = interceptors;
            this.tinyJdbcRuntime = new TinyJdbcRuntime(false, "verify", null, false, null, null, null);
        }

        /**
         * 获取测试运行时上下文。
         *
         * @return 测试运行时上下文
         */
        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() {
            return this.tinyJdbcRuntime;
        }

        /**
         * 获取测试 JDBC 模板。
         *
         * @return 测试 JDBC 模板
         */
        @Override
        protected JdbcTemplate getJdbcTemplate() {
            return this.jdbcTemplate;
        }

        /**
         * 获取测试分页处理器。
         *
         * @return 测试分页处理器
         */
        @Override
        protected IPageHandle getPageHandle() {
            return null;
        }

        /**
         * 获取测试 SQL 拦截器列表。
         *
         * @return 测试 SQL 拦截器列表
         */
        @Override
        protected List<SqlInterceptor> getSqlInterceptors() {
            return this.interceptors;
        }

        /**
         * 获取测试命名参数 JDBC 模板。
         *
         * @return 测试命名参数 JDBC 模板
         */
        @Override
        protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
            return null;
        }
    }
}
