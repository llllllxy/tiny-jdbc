package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 使用 main 方法快速验证 LambdaUpdateCriteria 的行为。
 */
public class LambdaUpdateCriteriaVerifyMain {

    @Test public void testAll() {
        LocalDateTime t1 = LocalDateTime.of(2026, 4, 18, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 4, 18, 10, 30);

        LambdaUpdateCriteria<VerifyDemoEntity> criteria = new LambdaUpdateCriteria<>();
        criteria.set(VerifyDemoEntity::getUpdateTime, t1);
        criteria.set(VerifyDemoEntity::getUpdateTime, t2);
        criteria.set(VerifyDemoEntity::getUpdateUserId, 11L);
        criteria.setDecrement(VerifyDemoEntity::getUpdateUserId, 3);
        criteria.eq(VerifyDemoEntity::getId, 99L);

        String updateSql = criteria.updateSql();
        List<Object> params = criteria.getParameters();

        System.out.println("updateSql =  " + updateSql);
        System.out.println("whereSql =  " + criteria.whereSql());
        System.out.println("params =  " + params);

        assertTrue(criteria.hasUpdateColumn("update_time"), "update_time should exist");
        assertTrue(criteria.hasUpdateColumn("update_user_id"), "update_user_id should exist");
        assertTrue(!criteria.hasUpdateColumn("create_time"), "create_time should not exist");

        assertTrue(updateSql.contains("update_time = ?"), "update_time placeholder SQL missing");
        assertTrue(updateSql.contains("update_user_id = update_user_id - 3"), "decrement SQL should keep latest");

        assertEquals(2, params.size(), "parameter size mismatch");
        assertEquals(t2, params.get(0), "update_time should keep latest value");
        assertEquals(99L, params.get(1), "where id parameter mismatch");

        System.out.println("LambdaUpdateCriteriaVerifyMain passed.");
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
