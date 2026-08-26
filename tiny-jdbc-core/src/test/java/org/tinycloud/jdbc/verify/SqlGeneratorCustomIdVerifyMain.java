package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.id.IdGeneratorInterface;
import org.tinycloud.jdbc.support.SqlGenerator;
import org.tinycloud.jdbc.support.SqlProvider;

/**
 * 使用 main 方法快速验证 CUSTOM 主键策略的运行时配置校验。
 */
public class SqlGeneratorCustomIdVerifyMain {

    /**
     * 验证 CUSTOM 主键策略在未配置和已配置自定义生成器时的行为。
     *
     * @param args 命令行参数
     */
    @Test public void testAll() {
        TinyJdbcRuntime missingRuntime = createRuntime(null);
        VerifyCustomIdEntity missingConfigEntity = new VerifyCustomIdEntity();
        missingConfigEntity.name = "missingConfig";
        assertThrows(() -> SqlGenerator.insertSql(missingConfigEntity, true, null, missingRuntime), "missing IdGeneratorInterface should throw");

        TinyJdbcRuntime configuredRuntime = createRuntime(entity -> 1001L);
        VerifyCustomIdEntity entity = new VerifyCustomIdEntity();
        entity.name = "configured";
        SqlProvider sqlProvider = SqlGenerator.insertSql(entity, true, null, configuredRuntime);

        assertEquals(1001L, entity.id, "custom id should inject into entity");
        assertEquals("INSERT INTO t_verify_custom_id (id,name) VALUES (?,?)", sqlProvider.getSql(), "insert SQL mismatch");
        assertEquals(1001L, sqlProvider.getParameters().get(0), "id parameter mismatch");
        assertEquals("configured", sqlProvider.getParameters().get(1), "name parameter mismatch");

        System.out.println("SqlGeneratorCustomIdVerifyMain passed.");
        System.out.println("sql = " + sqlProvider.getSql());
        System.out.println("params = " + sqlProvider.getParameters());
    }

    /**
     * 创建用于验证的 TinyJDBC 运行时上下文。
     *
     * @param idGeneratorInterface 自定义 ID 生成器，可为 null
     * @return TinyJDBC 运行时上下文
     */
    private static TinyJdbcRuntime createRuntime(IdGeneratorInterface idGeneratorInterface) {
        return new TinyJdbcRuntime(false, "verify", null, false, idGeneratorInterface, null, null);
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

    /**
     * CUSTOM 主键策略验证实体。
     */
    @Table("t_verify_custom_id")
    private static class VerifyCustomIdEntity {

        @Id(idType = IdType.CUSTOM)
        @Column("id")
        private Long id;

        @Column("name")
        private String name;
    }
}
