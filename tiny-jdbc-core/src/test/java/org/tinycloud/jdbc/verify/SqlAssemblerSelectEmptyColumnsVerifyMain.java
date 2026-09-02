package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlAssembler;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SqlAssembler.buildSelectSql 全字段 exist=false 时的边界校验测试。
 */
public class SqlAssemblerSelectEmptyColumnsVerifyMain {

    @Table("t_all_ignored")
    public static class AllIgnoredEntity {
        @Column(value = "id", exist = false)
        private Long id;
    }

    @Test
    public void testSelectSqlAllColumnsIgnoredThrows() {
        AllIgnoredEntity entity = new AllIgnoredEntity();
        try {
            SqlAssembler.buildSelectSql(entity);
            fail("expected TinyJdbcException when all columns exist=false");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("exist=false"));
        }
    }
}
