package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlGenerator;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.List;

/**
 * 使用 main 方法快速验证 updateById SQL 生成逻辑。
 */
public class SqlGeneratorUpdateByIdVerifyMain {

    /**
     * 验证 updateById 在有更新字段和没有更新字段时的行为。
     *
     * @param args 命令行参数
     */
    @Test public void testAll() {
        VerifyDemoEntity entity = new VerifyDemoEntity();
        entity.setId(100L);
        entity.setUpdateUserId(200L);

        SqlProvider sqlProvider = SqlGenerator.updateByIdSql(entity, true);
        List<Object> parameters = sqlProvider.getParameters();

        assertEquals("UPDATE t_verify_demo SET update_user_id=? WHERE id=?", sqlProvider.getSql(), "update SQL mismatch");
        assertEquals(2, parameters.size(), "parameter size mismatch");
        assertEquals(200L, parameters.get(0), "update_user_id parameter mismatch");
        assertEquals(100L, parameters.get(1), "id parameter mismatch");

        VerifyDemoEntity emptyUpdateEntity = new VerifyDemoEntity();
        emptyUpdateEntity.setId(101L);
        assertThrows(() -> SqlGenerator.updateByIdSql(emptyUpdateEntity, true), "empty update columns should throw");

        System.out.println("SqlGeneratorUpdateByIdVerifyMain passed.");
        System.out.println("sql = " + sqlProvider.getSql());
        System.out.println("params = " + parameters);
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
