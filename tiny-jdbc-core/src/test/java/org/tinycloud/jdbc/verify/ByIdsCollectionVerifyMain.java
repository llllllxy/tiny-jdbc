package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@code selectByIds} / {@code deleteByIds} 的集合入参测试。
 * <p>
 * 2.0.3 起两个方法由 {@code List<ID>} 改为 {@code Collection<? extends ID>}（破坏性变更），
 * 目的是让调用方直接传 {@code List} / {@code Set} 等任意集合，无需手工转换；
 * 同时 {@code ? extends ID} 使主键声明为 {@code Serializable} 时也能传入 {@code Set<Long>}
 * （若写成 {@code Collection<ID>}，因泛型不变性仍会编译失败）。
 * </p>
 */
public class ByIdsCollectionVerifyMain {

    /**
     * 记录最终执行 SQL 与参数的 JDBC 模板桩。
     */
    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private String sql;
        private Object[] args;

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            this.args = args;
            return Collections.emptyList();
        }

        @Override
        public int update(String sql, Object... args) {
            this.sql = sql;
            this.args = args;
            return args == null ? 0 : args.length;
        }

        String getSql() {
            return this.sql;
        }

        Object[] getArgs() {
            return this.args;
        }
    }

    /**
     * 主键声明为 {@code Serializable}，模拟代码生成器无法推断具体主键类型时的结果。
     */
    private static class SerializableIdSupport extends AbstractSqlSupport<VerifyDemoEntity, Serializable> {
        private final RecordingJdbcTemplate jdbcTemplate;

        private SerializableIdSupport(RecordingJdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() {
            return new TinyJdbcRuntime(false, "verify", null, false, null, null, null);
        }

        @Override
        protected JdbcTemplate getJdbcTemplate() {
            return this.jdbcTemplate;
        }

        @Override
        protected IPageHandle getPageHandle() {
            return null;
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

    /**
     * 验证：主键声明为 Serializable 时，Set / List / 任意 Collection 均可直接传入。
     */
    @Test
    public void testAcceptsAnyCollectionWhenIdIsSerializable() {
        String expectedSql = "DELETE FROM t_verify_demo WHERE id IN (?,?)";

        // HashSet（无序）
        RecordingJdbcTemplate t1 = new RecordingJdbcTemplate();
        SerializableIdSupport s1 = new SerializableIdSupport(t1);
        assertEquals(2, s1.deleteByIds(new java.util.HashSet<>(Arrays.asList(1L, 2L))));
        assertEquals(expectedSql, t1.getSql());
        assertEquals(2, t1.getArgs().length);

        // LinkedHashSet（有序）
        RecordingJdbcTemplate t2 = new RecordingJdbcTemplate();
        SerializableIdSupport s2 = new SerializableIdSupport(t2);
        s2.deleteByIds(new LinkedHashSet<>(Arrays.asList(1L, 2L)));
        assertEquals(expectedSql, t2.getSql());
        assertEquals(Arrays.asList(1L, 2L), Arrays.asList(t2.getArgs()));

        // TreeSet（排序）
        RecordingJdbcTemplate t3 = new RecordingJdbcTemplate();
        SerializableIdSupport s3 = new SerializableIdSupport(t3);
        s3.deleteByIds(new TreeSet<>(Arrays.asList(2L, 1L)));
        assertEquals(Arrays.asList(1L, 2L), Arrays.asList(t3.getArgs()));

        // List
        RecordingJdbcTemplate t4 = new RecordingJdbcTemplate();
        new SerializableIdSupport(t4).deleteByIds(Arrays.asList(1L, 2L));
        assertEquals(expectedSql, t4.getSql());

        // 声明为 Collection 变量的引用
        RecordingJdbcTemplate t5 = new RecordingJdbcTemplate();
        Collection<Long> asCollection = new ArrayList<>(Arrays.asList(1L, 2L));
        new SerializableIdSupport(t5).deleteByIds(asCollection);
        assertEquals(Arrays.asList(1L, 2L), Arrays.asList(t5.getArgs()));

        // 可变参数重载仍可用
        RecordingJdbcTemplate t6 = new RecordingJdbcTemplate();
        new SerializableIdSupport(t6).deleteByIds(1L, 2L);
        assertEquals(expectedSql, t6.getSql());
    }

    /**
     * 验证：selectByIds 同样接受 Set。
     */
    @Test
    public void testSelectByIdsAcceptsSet() {
        RecordingJdbcTemplate t = new RecordingJdbcTemplate();
        SerializableIdSupport support = new SerializableIdSupport(t);
        Set<Long> ids = new LinkedHashSet<>(Arrays.asList(1L, 2L));

        assertTrue(support.selectByIds(ids).isEmpty());
        assertTrue(t.getSql().startsWith("SELECT "));
        assertTrue(t.getSql().endsWith(" WHERE id IN (?,?)"));
        assertEquals(Arrays.asList(1L, 2L), Arrays.asList(t.getArgs()));
    }

    /**
     * 验证：null 与空集合给出明确异常，而不是走到 SQL 组装层。
     */
    @Test
    public void testRejectsNullAndEmptyCollection() {
        SerializableIdSupport support = new SerializableIdSupport(new RecordingJdbcTemplate());

        try {
            support.deleteByIds((Collection<Serializable>) null);
            fail("expected TinyJdbcException for null collection");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }

        try {
            support.deleteByIds(Collections.<Serializable>emptyList());
            fail("expected TinyJdbcException for empty collection");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }

        try {
            support.selectByIds(Collections.<Serializable>emptySet());
            fail("expected TinyJdbcException for empty set");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("cannot be null or empty"));
        }
    }

    /**
     * 验证：SQL 占位符数量与集合大小一致，不因集合实现不同而变化。
     */
    @Test
    public void testPlaceholderCountMatchesCollectionSize() {
        for (int size = 1; size <= 3; size++) {
            List<Long> ids = new ArrayList<>();
            for (int i = 1; i <= size; i++) {
                ids.add((long) i);
            }
            RecordingJdbcTemplate t = new RecordingJdbcTemplate();
            new SerializableIdSupport(t).deleteByIds(new LinkedHashSet<>(ids));

            String sql = t.getSql();
            int placeholders = sql.length() - sql.replace("?", "").length();
            assertEquals("placeholder count should match collection size " + size, size, placeholders);
            assertEquals(size, t.getArgs().length);
        }
    }
}
