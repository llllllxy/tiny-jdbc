package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.Arrays;

/**
 * 验证 Criteria 的 AND / OR 连接状态。
 */
public class CriteriaAndOrVerifyMain {

    @Test
    public void testAll() {
        verifyStringOrThenAndNested();
        verifyStringSkippedConditionClearsOr();
        verifyStringEmptyNestedConditionClearsOr();
        verifyStringInVarargsNullHandling();
        verifyStringNestedClauseRejected();
        verifyLambdaOrThenAndNested();
        verifyLambdaSkippedConditionClearsOr();
        verifyLambdaInVarargsNullHandling();
        verifyLambdaNestedClauseRejected();
        System.out.println("CriteriaAndOrVerifyMain passed.");
    }

    /**
     * {@code or().and(...)} 中的 AND 块应消费待连接的 OR，而不是硬编码为 AND。
     */
    private static void verifyStringOrThenAndNested() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .or()
                .and(group -> group.eq("update_user_id", 200L).gt("id", 10L))
                .eq("id", 20L);

        assertEquals(" WHERE create_user_id = ? OR (update_user_id = ? AND id > ?) AND id = ?", criteria.whereConditions());
        assertEquals(Arrays.asList(100L, 200L, 10L, 20L), criteria.getConditionParameters());
    }

    /**
     * 跳过 OR 后的条件时，OR 状态不得泄漏到后续条件。
     */
    private static void verifyStringSkippedConditionClearsOr() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .or()
                .eq(false, "update_user_id", 200L)
                .eq("id", 20L);

        assertEquals(" WHERE create_user_id = ? AND id = ?", criteria.whereConditions());
        assertEquals(Arrays.asList(100L, 20L), criteria.getConditionParameters());
    }

    /**
     * 空嵌套条件等同于跳过条件，不得让前置 OR 泄漏到后续条件。
     */
    private static void verifyStringEmptyNestedConditionClearsOr() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .or()
                .and(group -> { })
                .eq("id", 20L);

        assertEquals(" WHERE create_user_id = ? AND id = ?", criteria.whereConditions());
        assertEquals(Arrays.asList(100L, 20L), criteria.getConditionParameters());
    }

    /**
     * Lambda 版 {@code or().and(...)} 也应消费待连接的 OR。
     */
    private static void verifyLambdaOrThenAndNested() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getCreateUserId, 100L)
                .or()
                .and(group -> group.eq(VerifyDemoEntity::getUpdateUserId, 200L).gt(VerifyDemoEntity::getId, 10L))
                .eq(VerifyDemoEntity::getId, 20L);

        assertEquals(" WHERE create_user_id = ? OR (update_user_id = ? AND id > ?) AND id = ?", criteria.whereConditions());
        assertEquals(Arrays.asList(100L, 200L, 10L, 20L), criteria.getConditionParameters());
    }

    /**
     * Lambda 版跳过 OR 后的条件时也不得泄漏连接状态。
     */
    private static void verifyLambdaSkippedConditionClearsOr() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getCreateUserId, 100L)
                .or()
                .eq(false, VerifyDemoEntity::getUpdateUserId, 200L)
                .eq(VerifyDemoEntity::getId, 20L);

        assertEquals(" WHERE create_user_id = ? AND id = ?", criteria.whereConditions());
        assertEquals(Arrays.asList(100L, 20L), criteria.getConditionParameters());
    }

    /**
     * {@code in/notIn} 可变参数收到 {@code (Object[]) null} 时不得抛 NPE：
     * {@code whether=false} 直接跳过且不校验，{@code whether=true} 给出明确的 TinyJdbcException。
     */
    private static void verifyStringInVarargsNullHandling() {
        QueryCriteria<VerifyDemoEntity> skipped = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .in(false, "id", (Object[]) null)
                .notIn(false, "update_user_id", (Object[]) null)
                .eq("id", 20L);

        assertEquals(" WHERE create_user_id = ? AND id = ?", skipped.whereConditions());
        assertEquals(Arrays.asList(100L, 20L), skipped.getConditionParameters());

        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().in(true, "id", (Object[]) null),
                "IN with (Object[]) null should throw TinyJdbcException");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().notIn(true, "id", (Object[]) null),
                "NOT IN with (Object[]) null should throw TinyJdbcException");
    }

    /**
     * 嵌套 and/or 不允许使用只在顶层生效的子句：HAVING 参数会进入参数列表但不会被渲染，造成错位。
     */
    private static void verifyStringNestedClauseRejected() {
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>()
                        .eq("create_user_id", 100L)
                        .and(group -> group.having("count(*) > ?", 5)),
                "nested having should be rejected");

        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>()
                        .eq("create_user_id", 100L)
                        .or(group -> group.last("FOR UPDATE")),
                "nested last should be rejected");
    }

    /**
     * Lambda 版的 {@code in/notIn} 空值处理与字符串版一致。
     */
    private static void verifyLambdaInVarargsNullHandling() {
        LambdaQueryCriteria<VerifyDemoEntity> skipped = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getCreateUserId, 100L)
                .in(false, VerifyDemoEntity::getId, (Object[]) null)
                .eq(VerifyDemoEntity::getId, 20L);

        assertEquals(" WHERE create_user_id = ? AND id = ?", skipped.whereConditions());
        assertEquals(Arrays.asList(100L, 20L), skipped.getConditionParameters());

        assertThrows(() -> new LambdaQueryCriteria<VerifyDemoEntity>().in(true, VerifyDemoEntity::getId, (Object[]) null),
                "lambda IN with (Object[]) null should throw TinyJdbcException");
    }

    /**
     * Lambda 版的嵌套子句限制与字符串版一致。
     */
    private static void verifyLambdaNestedClauseRejected() {
        assertThrows(() -> new LambdaQueryCriteria<VerifyDemoEntity>()
                        .eq(VerifyDemoEntity::getCreateUserId, 100L)
                        .and(group -> group.having("count(*) > ?", 5)),
                "lambda nested having should be rejected");
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (TinyJdbcException e) {
            return;
        }
        throw new IllegalStateException("expected TinyJdbcException: " + message);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("expected=" + expected + ", actual=" + actual);
        }
    }
}
