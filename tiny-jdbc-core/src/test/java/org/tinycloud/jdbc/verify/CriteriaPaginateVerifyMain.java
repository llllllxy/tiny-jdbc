package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.page.MysqlPageHandleImpl;
import org.tinycloud.jdbc.page.Page;
import org.tinycloud.jdbc.support.AbstractSqlSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 验证条件构造器分页：COUNT 查询不携带 ORDER BY / last()，数据查询仍保留排序并由方言追加分页片段。
 */
public class CriteriaPaginateVerifyMain {

    @Test
    public void testStringCriteriaCountIgnoresTail() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestSupport support = new TestSupport(jdbc);

        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .orderByDesc("create_user_id")
                .last("FOR UPDATE");
        support.paginate(criteria, new Page<VerifyDemoEntity>(1L, 10L));

        assertEquals("SELECT COUNT(*) FROM t_verify_demo WHERE create_user_id = ?", jdbc.countSql, "count sql mismatch");
        assertEquals(Arrays.asList(100L), Arrays.asList(jdbc.countParams), "count params mismatch");
        assertTrue(jdbc.dataSql.contains("ORDER BY create_user_id DESC"), "data sql should keep order by");
        assertTrue(jdbc.dataSql.contains("FOR UPDATE LIMIT ?"), "data sql should keep tail and paging");
    }

    @Test
    public void testLambdaCriteriaCountIgnoresTail() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestSupport support = new TestSupport(jdbc);

        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getCreateUserId, 100L)
                .orderByDesc(VerifyDemoEntity::getCreateUserId)
                .last("FOR UPDATE");
        support.paginate(criteria, new Page<VerifyDemoEntity>(1L, 10L));

        assertEquals("SELECT COUNT(*) FROM t_verify_demo WHERE create_user_id = ?", jdbc.countSql, "count sql mismatch");
        assertEquals(Arrays.asList(100L), Arrays.asList(jdbc.countParams), "count params mismatch");
        assertTrue(jdbc.dataSql.contains("ORDER BY create_user_id DESC"), "data sql should keep order by");
    }

    @Test
    public void testGroupByHavingCountUsesSubQuery() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestSupport support = new TestSupport(jdbc);

        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .groupBy("create_user_id")
                .having("count(*) > ?", 5)
                .orderByDesc("create_user_id");
        support.paginate(criteria, new Page<VerifyDemoEntity>(1L, 10L));

        assertTrue(jdbc.countSql.startsWith("SELECT COUNT(*) FROM (SELECT 1 FROM t_verify_demo"),
                "grouped count should use SELECT 1 sub query");
        assertTrue(jdbc.countSql.contains("GROUP BY create_user_id HAVING count(*) > ?"), "grouped count should keep grouping");
        assertFalse(jdbc.countSql.contains("ORDER BY"), "count sql should not contain order by");
        assertEquals(Arrays.asList(100L, 5), Arrays.asList(jdbc.countParams), "count params mismatch");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    /** 记录 COUNT 与数据查询 SQL、参数，不真正访问数据库。 */
    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private String countSql;
        private Object[] countParams;
        private String dataSql;
        private Object[] dataParams;

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            this.countSql = sql;
            this.countParams = args;
            return requiredType.cast(2L);
        }

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.dataSql = sql;
            this.dataParams = args;
            return new ArrayList<T>();
        }
    }

    /** 提供 AbstractSqlSupport 所需依赖的测试实现。 */
    private static class TestSupport extends AbstractSqlSupport<VerifyDemoEntity, Long> {
        private final JdbcTemplate jdbcTemplate;
        private final TinyJdbcRuntime runtime =
                new TinyJdbcRuntime(false, "verify", null, false, null, null, null);

        private TestSupport(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        protected JdbcTemplate getJdbcTemplate() {
            return this.jdbcTemplate;
        }

        @Override
        protected IPageHandle getPageHandle() {
            return new MysqlPageHandleImpl();
        }

        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() {
            return this.runtime;
        }

        @Override
        protected List<SqlInterceptor> getSqlInterceptors() {
            return Collections.emptyList();
        }

        @Override
        protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
            return null;
        }
    }
}
