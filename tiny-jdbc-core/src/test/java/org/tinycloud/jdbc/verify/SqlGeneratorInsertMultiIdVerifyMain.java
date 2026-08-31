package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlGenerator;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SqlGenerator.insertSql 对多 @Id 的校验测试：多个主键字段时应报错（与 updateByIdSql 一致）。
 */
public class SqlGeneratorInsertMultiIdVerifyMain {

    @Table("t_multi_id")
    public static class MultiIdEntity {
        @Id(idType = IdType.INPUT)
        private Long idA;
        @Id(idType = IdType.INPUT)
        private Long idB;
        @Column("name")
        private String name;

        public Long getIdA() {
            return idA;
        }

        public void setIdA(Long idA) {
            this.idA = idA;
        }

        public Long getIdB() {
            return idB;
        }

        public void setIdB(Long idB) {
            this.idB = idB;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testInsertSqlRejectsMultipleId() {
        MultiIdEntity entity = new MultiIdEntity();
        // 第一个主键有值，能安全走到第二个 @Id，从而触发多主键校验
        entity.setIdA(1L);
        entity.setIdB(2L);
        entity.setName("x");
        try {
            SqlGenerator.insertSql(entity, false, null, null);
            fail("expected TinyJdbcException for multiple @Id");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("Only one @Id"));
        }
    }
}
