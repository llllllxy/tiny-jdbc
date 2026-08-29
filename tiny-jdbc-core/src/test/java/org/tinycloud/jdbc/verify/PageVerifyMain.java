package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.DB2PageHandleImpl;
import org.tinycloud.jdbc.page.Oracle12cPageHandleImpl;
import org.tinycloud.jdbc.page.PageHandleFactory;
import org.tinycloud.jdbc.page.PagingSQLProvider;
import org.tinycloud.jdbc.page.PostgreSqlPageHandleImpl;
import org.tinycloud.jdbc.util.DbType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 分页模块的测试：DB2 方言正确性、未知数据库类型显式报错。
 */
public class PageVerifyMain {

    // 验证：DB2 使用 ROW_NUMBER() OVER (ORDER BY 1)，且不再出现错误的 ROWNUMBER()
    @Test
    public void testDb2PagingSqlUsesRowNumber() {
        DB2PageHandleImpl handler = new DB2PageHandleImpl();
        PagingSQLProvider p = handler.handlerPagingSQL("SELECT * FROM t", 2, 10);
        assertTrue(p.getSql().contains("ROW_NUMBER() OVER (ORDER BY 1)"));
        assertFalse(p.getSql().contains("ROWNUMBER"));
        // pageStart = (2-1)*10+1 = 11, pageEnd = 11+10-1 = 20
        assertEquals(11L, ((Number) p.getParameters()[0]).longValue());
        assertEquals(20L, ((Number) p.getParameters()[1]).longValue());
    }

    // 验证：DB2 count 语句包裹原 SQL
    @Test
    public void testDb2CountSql() {
        DB2PageHandleImpl handler = new DB2PageHandleImpl();
        assertEquals("SELECT COUNT(*) FROM ( SELECT * FROM t ) TEMP", handler.handlerCountSQL("SELECT * FROM t"));
    }

    // 验证：不支持的数据库类型（SQLServer2005/Sybase/Hive2/OTHER）显式抛异常，不再静默套用 PG
    @Test
    public void testUnsupportedDbTypeThrows() {
        assertThrowsNotSupported(DbType.SQLSERVER_2005);
        assertThrowsNotSupported(DbType.SYBASE);
        assertThrowsNotSupported(DbType.HIVE2);
        assertThrowsNotSupported(DbType.OTHER);
    }

    // 验证：已支持的数据库类型返回对应处理器
    @Test
    public void testSupportedDbTypeReturnsHandler() {
        assertTrue(PageHandleFactory.createPageHandleByDbType(DbType.POSTGRE_SQL) instanceof PostgreSqlPageHandleImpl);
        assertTrue(PageHandleFactory.createPageHandleByDbType(DbType.DB2) instanceof DB2PageHandleImpl);
        assertTrue(PageHandleFactory.createPageHandleByDbType(DbType.SQLSERVER) instanceof Oracle12cPageHandleImpl);
    }

    private void assertThrowsNotSupported(DbType type) {
        try {
            PageHandleFactory.createPageHandleByDbType(type);
            fail("Expected TinyJdbcException for " + type.getName());
        } catch (TinyJdbcException e) {
            assertTrue("message should mention not supported: " + e.getMessage(), e.getMessage().contains("not supported"));
        }
    }
}
