package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;

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
        verifyLambdaOrThenAndNested();
        verifyLambdaSkippedConditionClearsOr();
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

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("expected=" + expected + ", actual=" + actual);
        }
    }
}
