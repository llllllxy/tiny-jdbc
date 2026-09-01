package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.DB2PageHandleImpl;
import org.tinycloud.jdbc.page.MysqlPageHandleImpl;
import org.tinycloud.jdbc.page.OffsetPage;
import org.tinycloud.jdbc.page.Oracle12cPageHandleImpl;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.page.PageCheck;
import org.tinycloud.jdbc.page.PageHandleFactory;
import org.tinycloud.jdbc.page.PagingSQLProvider;
import org.tinycloud.jdbc.page.PostgreSqlPageHandleImpl;
import org.tinycloud.jdbc.util.DbType;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 分页模块的测试：DB2 方言正确性、未知数据库类型显式报错。
 */
public class PageVerifyMain {

    // 验证：DB2 使用 ROWNUMBER() OVER()（与 MyBatis-Plus / PageHelper 保持一致的写法）
    @Test
    public void testDb2PagingSqlUsesRowNumber() {
        DB2PageHandleImpl handler = new DB2PageHandleImpl();
        PagingSQLProvider p = handler.handlerPagingSQL("SELECT * FROM t", 2, 10);
        assertTrue(p.getSql().contains("ROWNUMBER() OVER()"));
        assertFalse(p.getSql().contains("ROW_NUMBER"));
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

    // ======================== 分页参数边界 ========================

    // 验证：非法页码（<=0）直接被 handler 拒绝，而不是生成越界 offset
    @Test
    public void testInvalidPageNoThrowsInHandler() {
        try {
            new MysqlPageHandleImpl().handlerPagingSQL("SELECT * FROM t", 0, 10);
            fail("Expected TinyJdbcException for pageNo=0");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("pageNum"));
        }
    }

    // 验证：非法页大小（<=0）直接被 handler 拒绝，而不是除零/生成负 offset
    @Test
    public void testInvalidPageSizeThrowsInHandler() {
        try {
            new MysqlPageHandleImpl().handlerPagingSQL("SELECT * FROM t", 1, 0);
            fail("Expected TinyJdbcException for pageSize=0");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("pageSize"));
        }
    }

    // 验证：offset 溢出（(pageNo-1)*pageSize 超 long）被捕获并转为明确异常
    @Test
    public void testOffsetOverflowThrows() {
        try {
            PageCheck.offset(Long.MAX_VALUE, 2L);
            fail("Expected TinyJdbcException for offset overflow");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("overflow"));
        }
    }

    // 验证：pageEnd 溢出同样被捕获
    @Test
    public void testPageEndOverflowThrows() {
        try {
            PageCheck.pageEnd(Long.MAX_VALUE, 2L);
            fail("Expected TinyJdbcException for pageEnd overflow");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("overflow"));
        }
    }

    // 验证：正常的 offset / pageEnd 计算正确
    @Test
    public void testOffsetAndPageEndComputedCorrectly() {
        assertEquals(0L, PageCheck.offset(1L, 10L));
        assertEquals(10L, PageCheck.offset(2L, 10L));
        assertEquals(20L, PageCheck.offset(3L, 10L));
        assertEquals(20L, PageCheck.pageEnd(2L, 10L));
    }

    // 验证：Page.total 为 null / pageSize 为 null 时不再 NPE，pages 安全返回
    @Test
    public void testPageTotalNullOrPageSizeNullSafe() {
        Page<Object> empty = new Page<>(new ArrayList<>(), null, null, null);
        assertNull(empty.getPages());

        Page<Object> p = new Page<>(1L, 10L);
        p.setTotal(null);
        assertNull(p.getPages());
    }

    // 验证：pageSize 为 0 时 setTotal 抛业务异常而非除零
    @Test
    public void testPageZeroPageSizeThrows() {
        Page<Object> p = new Page<>(1L, 0L);
        try {
            p.setTotal(100L);
            fail("Expected TinyJdbcException for pageSize=0");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("pageSize"));
        }
    }

    // 验证：正常 total/pageSize 下 pages 计算正确（含溢出安全的 ceil 公式）
    @Test
    public void testPagesComputedCorrectly() {
        assertEquals(0L, (long) PageCheck.pages(0L, 10L));
        assertEquals(10L, (long) PageCheck.pages(100L, 10L));
        assertEquals(11L, (long) PageCheck.pages(101L, 10L));
        assertEquals(10L, (long) PageCheck.pages(95L, 10L));
    }

    // 验证：OffsetPage 提供 getPages() 且计算正确，null/0 情况下不再 NPE
    @Test
    public void testOffsetPageGetPagesAndNullSafe() {
        OffsetPage<Object> op = new OffsetPage<>(new ArrayList<>(), 100L, 0L, 10L);
        assertNotNull(op.getPages());
        assertEquals(10L, (long) op.getPages());

        OffsetPage<Object> empty = new OffsetPage<>(new ArrayList<>(), null, null, null);
        assertNull(empty.getPages());
    }

    // 验证：通过 IPageHandle.handle 的默认入口同样会拦截非法参数（直接调用 handler 而非 paginate 入口）
    @Test
    public void testHandleRejectsInvalidParams() {
        try {
            new MysqlPageHandleImpl().handle("SELECT * FROM t", 0, 10);
            fail("Expected TinyJdbcException for pageNo=0 via handle()");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("pageNum"));
        }
    }
}
