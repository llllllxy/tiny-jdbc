package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.core.annotation.Order;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.interceptor.SqlExecutor;
import org.tinycloud.jdbc.interceptor.SqlRequest;
import org.tinycloud.jdbc.interceptor.SqlInterceptorChain;
import org.tinycloud.jdbc.interceptor.SqlType;
import org.tinycloud.jdbc.sql.RawSql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 针对测试审查发现的零覆盖分支做集中回归：
 * orderBy 的 boolean 重载、last(RawSql) 安全闸门、checkTailSql 括号配对、
 * SqlRequest 参数双向副本、拦截器 @Order 排序与短路。
 */
public class CoverageGapsVerifyMain {

    @Test
    public void testAll() {
        verifyStringOrderByBooleanOverloads();
        verifyLambdaOrderByBooleanOverloads();
        verifyStringLastOverwriteAndRawSql();
        verifyLambdaLastOverwriteAndRawSql();
        verifyTailSqlUnmatchedParenthesis();
        verifySqlRequestBiDirectionalCopies();
        verifySqlRequestNullArgsAndAttributes();
        verifyInterceptorOrderAndShortCircuit();
        System.out.println("CoverageGapsVerifyMain passed.");
    }

    private static void verifyStringOrderByBooleanOverloads() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .eq("id", 1L)
                .orderBy(false, "create_user_id", true)
                .orderBy(true, "update_user_id", false)
                .orderByDesc(false, "id");
        assertEquals(" WHERE id = ? ORDER BY update_user_id", c.whereSql(), "boolean orderBy mismatch");
        assertEquals(Collections.singletonList(1L), c.getConditionParameters(), "orderBy must not bind parameters");
    }

    private static void verifyLambdaOrderByBooleanOverloads() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getId, 1L)
                .orderBy(false, VerifyDemoEntity::getCreateUserId, true)
                .orderBy(true, VerifyDemoEntity::getUpdateUserId, false)
                .orderByDesc(false, VerifyDemoEntity::getId);
        assertEquals(" WHERE id = ? ORDER BY update_user_id", c.whereSql(), "lambda boolean orderBy mismatch");
        assertEquals(Collections.singletonList(1L), c.getConditionParameters(), "lambda orderBy must not bind parameters");
    }

    private static void verifyStringLastOverwriteAndRawSql() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .eq("id", 1L)
                .last("LIMIT 5")
                .last("LIMIT 10");
        assertEquals(" WHERE id = ? LIMIT 10", c.whereSql(), "last should overwrite previous value");

        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("LIMIT 1; DROP TABLE t"),
                "unsafe last should be rejected");
        QueryCriteria<VerifyDemoEntity> raw = new QueryCriteria<VerifyDemoEntity>()
                .eq("id", 1L).last(RawSql.wrap("LIMIT 1 UNION SELECT 1"));
        assertEquals(" WHERE id = ? LIMIT 1 UNION SELECT 1", raw.whereSql(), "RawSql should bypass tail check");
    }

    private static void verifyLambdaLastOverwriteAndRawSql() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getId, 1L)
                .last("LIMIT 5")
                .last("LIMIT 10");
        assertEquals(" WHERE id = ? LIMIT 10", c.whereSql(), "lambda last should overwrite previous value");

        assertThrows(() -> new LambdaQueryCriteria<VerifyDemoEntity>().last("LIMIT 1; DROP TABLE t"),
                "lambda unsafe last should be rejected");
        LambdaQueryCriteria<VerifyDemoEntity> raw = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getId, 1L).last(RawSql.wrap("LIMIT 1 UNION SELECT 1"));
        assertEquals(" WHERE id = ? LIMIT 1 UNION SELECT 1", raw.whereSql(), "lambda RawSql should bypass tail check");
    }

    private static void verifyTailSqlUnmatchedParenthesis() {
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("COUNT(*"),
                "unclosed parenthesis should be rejected");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("a)"),
                "leading close parenthesis should be rejected");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("(a))"),
                "extra close parenthesis should be rejected");
        // 括号平衡的合法片段不应被拒绝
        new QueryCriteria<VerifyDemoEntity>().last("(a = 1)");
    }

    private static void verifySqlRequestBiDirectionalCopies() {
        Object[] args = new Object[]{1L};
        SqlRequest<Long> request = new SqlRequest<Long>("SELECT ?", args, SqlType.QUERY);
        args[0] = 999L;
        assertEquals(1L, request.getArgs()[0], "constructor should copy inbound args");

        Object[] outbound = request.getArgs();
        outbound[0] = 888L;
        assertEquals(1L, request.getArgs()[0], "getArgs should return a copy");

        Object[] replacement = new Object[]{2L};
        request.setStatement("SELECT ?", replacement);
        replacement[0] = 777L;
        assertEquals(2L, request.getArgs()[0], "setStatement should copy args");
        assertEquals("SELECT ?", request.getSql(), "setStatement should update sql");

        List<Object[]> batchArgs = new ArrayList<>();
        batchArgs.add(new Object[]{1L});
        SqlRequest<Long> batch = SqlRequest.batch("INSERT INTO t VALUES (?)", batchArgs);
        batchArgs.get(0)[0] = 666L;
        assertEquals(1L, batch.getBatchArgs().get(0)[0], "batch constructor should copy inbound args");

        List<Object[]> outboundBatch = batch.getBatchArgs();
        outboundBatch.get(0)[0] = 555L;
        assertEquals(1L, batch.getBatchArgs().get(0)[0], "getBatchArgs should return a copy");
    }

    private static void verifySqlRequestNullArgsAndAttributes() {
        SqlRequest<Long> request = new SqlRequest<Long>("SELECT 1", null, SqlType.QUERY);
        assertEquals(0, request.getArgs().length, "null args should become empty array");
        assertEquals(SqlType.QUERY, request.getSqlType(), "sql type mismatch");
        request.putAttribute("k", "v");
        assertEquals("v", request.getAttribute("k"), "attribute mismatch");
        assertTrue(request.getAttribute("absent") == null, "absent attribute should be null");
    }

    private static void verifyInterceptorOrderAndShortCircuit() {
        List<String> calls = new ArrayList<String>();
        SqlInterceptor ordered1 = new FirstInterceptor(calls, false);
        SqlInterceptor ordered2 = new SecondInterceptor(calls);

        // 故意逆序放入，由生产 SqlExecutor 的 AnnotationAwareOrderComparator.sort 排序
        SqlExecutor executor = new SqlExecutor(Arrays.asList(ordered2, ordered1));
        String result = executor.execute(new SqlRequest<String>("SELECT 1", null, SqlType.QUERY),
                request -> {
                    calls.add("executed");
                    return "executed";
                });
        assertEquals("executed", result, "chain result mismatch");
        assertEquals(Arrays.asList("first", "second", "executed"), calls, "@Order should determine interceptor order");

        // 短路拦截器不调用 proceed，执行器不应被调用
        List<String> shortCalls = new ArrayList<String>();
        SqlInterceptor shortCircuit = new FirstInterceptor(shortCalls, true);
        new SqlExecutor(Collections.singletonList(shortCircuit)).execute(
                new SqlRequest<String>("SELECT 1", null, SqlType.QUERY),
                request -> {
                    shortCalls.add("executed");
                    return "executed";
                });
        assertEquals(Collections.singletonList("first"), shortCalls, "short-circuit should skip executor");
    }

    @Order(1)
    private static class FirstInterceptor implements SqlInterceptor {
        private final List<String> calls;
        private final boolean shortCircuit;

        FirstInterceptor(List<String> calls, boolean shortCircuit) {
            this.calls = calls;
            this.shortCircuit = shortCircuit;
        }

        @Override
        public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
            calls.add("first");
            return this.shortCircuit ? null : chain.proceed(request);
        }
    }

    @Order(2)
    private static class SecondInterceptor implements SqlInterceptor {
        private final List<String> calls;

        SecondInterceptor(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public <R> R intercept(SqlRequest<R> request, SqlInterceptorChain<R> chain) {
            calls.add("second");
            return chain.proceed(request);
        }
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (TinyJdbcException e) {
            return;
        }
        throw new IllegalStateException("expected TinyJdbcException: " + message);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }
}
