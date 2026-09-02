package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.Arrays;

/**
 * {@link SqlAssembler} 按主键查询/删除 SQL 生成的验证：
 * 1) selectByIdSql / selectByIdsSql / deleteByIdSql / deleteByIdsSql 直接读取 {@code getTableInfo} 的列与主键列；
 * 2) 实体未声明 {@code @Id} 时，这些方法（含 selectCriteriaSql 与 selectLambdaCriteriaSql 默认列）仍抛出原有明确异常，
 *    避免把 null 主键列拼进 SQL（"WHERE null=?"）。
 */
public class SqlAssemblerPrimaryKeyVerifyMain {

    @Table("t_pk")
    public static class PrimaryKeyEntity {
        @Id(idType = IdType.INPUT)
        @Column("id")
        private Long id;

        @Column("name")
        private String name;
    }

    @Table("t_no_pk")
    public static class NoPkEntity {
        @Column("id")
        private Long id;

        @Column("name")
        private String name;
    }

    @Test
    public void testSelectByIdSql() {
        SqlProvider so = SqlAssembler.buildSelectByIdSql(100L, PrimaryKeyEntity.class);
        assertEquals("SELECT id,name FROM t_pk WHERE id=?", so.getSql(), "selectById SQL mismatch");
        assertEquals(1, so.getParameters().size(), "selectById param size mismatch");
        assertEquals(100L, so.getParameters().get(0), "selectById param mismatch");
    }

    @Test
    public void testSelectByIdsSql() {
        SqlProvider so = SqlAssembler.buildSelectByIdsSql(PrimaryKeyEntity.class, Arrays.<Object>asList(1L, 2L));
        assertEquals("SELECT id,name FROM t_pk WHERE id IN (?,?)", so.getSql(), "selectByIds SQL mismatch");
        assertEquals(Arrays.asList(1L, 2L), so.getParameters(), "selectByIds params mismatch");
    }

    @Test
    public void testDeleteByIdSql() {
        SqlProvider so = SqlAssembler.buildDeleteByIdSql(100L, PrimaryKeyEntity.class);
        assertEquals("DELETE FROM t_pk WHERE id=?", so.getSql(), "deleteById SQL mismatch");
        assertEquals(1, so.getParameters().size(), "deleteById param size mismatch");
        assertEquals(100L, so.getParameters().get(0), "deleteById param mismatch");
    }

    @Test
    public void testDeleteByIdsSql() {
        SqlProvider so = SqlAssembler.buildDeleteByIdsSql(PrimaryKeyEntity.class, Arrays.<Object>asList(1L, 2L));
        assertEquals("DELETE FROM t_pk WHERE id IN (?,?)", so.getSql(), "deleteByIds SQL mismatch");
        assertEquals(Arrays.asList(1L, 2L), so.getParameters(), "deleteByIds params mismatch");
    }

    @Test
    public void testMissingPrimaryKeyThrowsExplicitError() {
        // 按主键查询/删除：主键缺失时应抛出原有明确异常，而不是生成 "WHERE null=?"
        assertThrows(() -> SqlAssembler.buildSelectByIdSql(1L, NoPkEntity.class), "selectByIdSql should reject missing @Id");
        assertThrows(() -> SqlAssembler.buildSelectByIdsSql(NoPkEntity.class, Arrays.<Object>asList(1L)), "selectByIdsSql should reject missing @Id");
        assertThrows(() -> SqlAssembler.buildDeleteByIdSql(1L, NoPkEntity.class), "deleteByIdSql should reject missing @Id");
        assertThrows(() -> SqlAssembler.buildDeleteByIdsSql(NoPkEntity.class, Arrays.<Object>asList(1L)), "deleteByIdsSql should reject missing @Id");

        // 条件构造器默认列（未显式指定 select 字段）同样保留原有主键校验
        assertThrows(() -> SqlAssembler.buildSelectCriteriaSql(new QueryCriteria<NoPkEntity>(), NoPkEntity.class), "selectCriteriaSql should reject missing @Id");
        assertThrows(() -> SqlAssembler.buildSelectLambdaCriteriaSql(new LambdaQueryCriteria<NoPkEntity>(), NoPkEntity.class), "selectLambdaCriteriaSql should reject missing @Id");
    }

    /**
     * 断言操作抛出 TinyJdbcException，且消息包含主键提示。
     */
    private static void assertThrows(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (TinyJdbcException e) {
            if (!e.getMessage().contains("Please correctly set the primary key attribute column!")) {
                throw new IllegalStateException("unexpected exception message: " + e.getMessage(), e);
            }
            return;
        }
        throw new IllegalStateException(message);
    }

    /**
     * 断言两个对象相等。
     */
    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }
}
