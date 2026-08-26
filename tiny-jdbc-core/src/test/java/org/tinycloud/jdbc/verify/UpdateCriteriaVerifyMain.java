package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.criteria.update.UpdateCriteria;

import java.util.List;

/**
 * 使用 main 方法快速验证 UpdateCriteria 的行为。
 */
public class UpdateCriteriaVerifyMain {

    @Test public void testAll() {
        UpdateCriteria<VerifyDemoEntity> criteria = new UpdateCriteria<>();
        criteria.set("update_time", "T1");
        criteria.set("update_time", "T2");
        criteria.set("remark", "R1");
        criteria.setIncrement("version", 1);
        criteria.setIncrement("version", 2);
        criteria.eq("id", 100L);

        String updateSql = criteria.updateSql();
        List<Object> params = criteria.getParameters();

        assertTrue(criteria.hasUpdateColumn("update_time"), "update_time should exist");
        assertTrue(criteria.hasUpdateColumn("remark"), "remark should exist");
        assertTrue(criteria.hasUpdateColumn("version"), "version should exist");
        assertTrue(!criteria.hasUpdateColumn("missing"), "missing should not exist");

        assertTrue(updateSql.contains("update_time = ?"), "update_time placeholder SQL missing");
        assertTrue(updateSql.contains("remark = ?"), "remark placeholder SQL missing");
        assertTrue(updateSql.contains("version = version + 2"), "version increment SQL should keep latest");

        assertEquals(3, params.size(), "parameter size mismatch");
        assertEquals("T2", params.get(0), "update_time should keep latest value");
        assertEquals("R1", params.get(1), "remark value mismatch");
        assertEquals(100L, params.get(2), "where id parameter mismatch");

        System.out.println("UpdateCriteriaVerifyMain passed.");

        System.out.println("updateSql =  " + updateSql);
        System.out.println("whereSql =  " + criteria.whereSql());
        System.out.println("params =  " + params);
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
