package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.util.TableParserUtils;

/**
 * 使用 main 方法快速验证多 @Id 标记会被明确拦截。
 */
public class SqlAssemblerMultiIdVerifyMain {

    /**
     * 验证表字段解析和 updateById SQL 生成不会静默接受多个 @Id。
     *
     * @param args 命令行参数
     */
    @Test public void testAll() {
        VerifyMultiIdEntity entity = new VerifyMultiIdEntity();
        entity.firstId = 1L;
        entity.secondId = 2L;
        entity.name = "multiId";

        assertThrows(() -> TableParserUtils.getTableInfo(VerifyMultiIdEntity.class), "getTableInfo should reject multiple @Id");
        assertThrows(() -> SqlAssembler.buildUpdateByIdSql(entity, true), "updateByIdSql should reject multiple @Id");

        System.out.println("SqlAssemblerMultiIdVerifyMain passed.");
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
            if (!e.getMessage().contains("Only one @Id is supported")) {
                throw new IllegalStateException("unexpected exception message: " + e.getMessage(), e);
            }
            return;
        }
        throw new IllegalStateException(message);
    }

    /**
     * 多主键验证实体。
     */
    @Table("t_verify_multi_id")
    private static class VerifyMultiIdEntity {

        @Id(idType = IdType.INPUT)
        @Column("first_id")
        private Long firstId;

        @Id(idType = IdType.INPUT)
        @Column("second_id")
        private Long secondId;

        @Column("name")
        private String name;
    }
}
