package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.SQL;

import java.util.Arrays;
import java.util.Collections;

/**
 * 使用 main 方法快速验证 IN/NOT IN 条件参数校验。
 */
public class InConditionVerifyMain {

    /**
     * 验证 Criteria、LambdaCriteria 和 SQL builder 对空集合、null 集合的保护。
     *
     * @param args 命令行参数
     */
    @Test public void testAll() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<>();
        assertThrows(() -> criteria.in("id", null), "criteria in null should throw");
        assertThrows(() -> criteria.notIn("id", Collections.emptyList()), "criteria notIn empty should throw");
        criteria.in(false, "id", null);

        LambdaUpdateCriteria<VerifyDemoEntity> lambdaCriteria = new LambdaUpdateCriteria<>();
        assertThrows(() -> lambdaCriteria.in(VerifyDemoEntity::getId, null), "lambda in null should throw");
        assertThrows(() -> lambdaCriteria.notIn(VerifyDemoEntity::getId, Collections.emptyList()), "lambda notIn empty should throw");
        lambdaCriteria.in(false, VerifyDemoEntity::getId, null);

        assertThrows(() -> SQL.table("t_verify_demo").select().where(w -> w.in("id", (java.util.Collection<?>) null)).toSql(), "sql builder in null should throw");
        assertThrows(() -> SQL.table("t_verify_demo").select().where(w -> w.notIn("id", Collections.emptyList())).toSql(), "sql builder notIn empty should throw");

        String whereSql = new QueryCriteria<VerifyDemoEntity>().in("id", Arrays.asList(1L, 2L)).whereSql();
        assertEquals(" WHERE id IN (?, ?)", whereSql, "valid in SQL mismatch");

        System.out.println("InConditionVerifyMain passed.");
    }

    /**
     * 断言操作会抛出 TinyJdbcException。
     *
     * @param runnable 待执行操作
     * @param message  断言失败提示
     */
    private static void assertThrows(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (TinyJdbcException e) {
            if (!e.getMessage().contains("cannot be null or empty")) {
                throw new IllegalStateException("unexpected exception message: " + e.getMessage(), e);
            }
            return;
        }
        throw new IllegalStateException(message);
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
}
