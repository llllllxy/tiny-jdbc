package org.tinycloud.jdbc.verify;

import org.junit.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.tinycloud.jdbc.config.TinyJdbcRuntime;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.interceptor.SqlInterceptor;
import org.tinycloud.jdbc.page.IPageHandle;
import org.tinycloud.jdbc.support.AbstractSqlSupport;
import org.tinycloud.jdbc.support.BatchInsertSql;
import org.tinycloud.jdbc.support.BatchMode;
import org.tinycloud.jdbc.support.SqlAssembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 批量插入 / 批量 upsert 的 SQL 生成与两种执行模式的回归验证。
 */
public class BatchInsertVerifyMain {

    @Test
    public void testAll() {
        verifyBuildBatchInsertStableColumnsAndAutoIncrement();
        verifyBuildBatchInsertNullColumnMismatchThrows();
        verifyBuildBatchInsertIncludesNullWhenNotIgnore();
        verifyMultiValueExecutionAndChunking();
        verifyJdbcBatchExecution();
        verifyRuntimeDefaultMode();
        System.out.println("BatchInsertVerifyMain passed.");
    }

    /**
     * 构建批量插入：自增主键应被剔除，列集稳定，行参数与列集顺序一致。
     */
    private static void verifyBuildBatchInsertStableColumnsAndAutoIncrement() {
        BatchVerifyEntity a = new BatchVerifyEntity();
        a.setName("Alice");
        a.setAge(20);
        a.setEmail("a@x.com");
        BatchVerifyEntity b = new BatchVerifyEntity();
        b.setName("Bob");
        b.setAge(25);
        b.setEmail("b@x.com");

        BatchInsertSql batch = SqlAssembler.buildBatchInsert(Arrays.asList(a, b), true,
                new JdbcTemplate(), runtime());

        assertEquals("t_batch_verify", batch.getTableName(), "table name mismatch");
        assertEquals(Arrays.asList("name", "age", "email"), batch.getColumns(), "auto-increment pk should be excluded");
        assertEquals("id", batch.getPrimaryKeyColumn(), "primary key column mismatch");
        assertEquals(true, batch.isAutoIncrement(), "auto-increment flag mismatch");
        assertEquals(2, batch.getRows().size(), "row count mismatch");
        assertEquals(Arrays.asList("Alice", 20, "a@x.com"), Arrays.asList(batch.getRows().get(0)), "first row mismatch");
        assertEquals(Arrays.asList("Bob", 25, "b@x.com"), Arrays.asList(batch.getRows().get(1)), "second row mismatch");
    }

    /**
     * ignoreNulls=true 时，首个实体的列集被固定，后续实体出现不同列集应抛异常。
     */
    private static void verifyBuildBatchInsertNullColumnMismatchThrows() {
        BatchVerifyEntity a = new BatchVerifyEntity();
        a.setName("Alice");
        a.setAge(20);
        a.setEmail("a@x.com");
        BatchVerifyEntity b = new BatchVerifyEntity();
        b.setName("Bob");
        b.setAge(25);
        // b.email 为 null，导致列集与首个不一致
        assertThrows(() -> SqlAssembler.buildBatchInsert(Arrays.asList(a, b), true,
                new JdbcTemplate(), runtime()), "batchInsert requires the same columns");
    }

    /**
     * ignoreNulls=false 时，null 字段应作为 SQL NULL 参数保留在行中。
     */
    private static void verifyBuildBatchInsertIncludesNullWhenNotIgnore() {
        BatchVerifyEntity a = new BatchVerifyEntity();
        a.setName("Alice");
        a.setAge(20);
        a.setEmail(null);

        BatchInsertSql batch = SqlAssembler.buildBatchInsert(Arrays.asList(a), false,
                new JdbcTemplate(), runtime());
        assertEquals(Arrays.asList("name", "age", "email"), batch.getColumns(), "columns should include all fields");
        assertEquals(Arrays.asList("Alice", 20, null), Arrays.asList(batch.getRows().get(0)), "null should be retained");
    }

    /**
     * MULTI_VALUE 模式：产生单条多值 INSERT，并按 batchInsertSize 切块。
     */
    private static void verifyMultiValueExecutionAndChunking() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestBatchSupport support = new TestBatchSupport(jdbc, multiValueRuntime());

        List<BatchVerifyEntity> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BatchVerifyEntity e = new BatchVerifyEntity();
            e.setName("u" + i);
            e.setAge(i);
            e.setEmail("u" + i + "@x.com");
            list.add(e);
        }

        int[] result = support.batchInsert(list, false);
        assertEquals(5, result.length, "result length mismatch");
        // batchInsertSize=2 -> 5 行切成 3 条语句
        assertEquals(3, jdbc.updateSqls.size(), "should be chunked into 3 statements");
        assertEquals(true, jdbc.updateSqls.get(0).contains("INSERT INTO t_batch_verify"), "sql should be an insert");
        assertEquals(true, jdbc.updateSqls.get(0).contains("(?,"), "first chunk should be multi-value");
        assertEquals(true, jdbc.updateSqls.get(2).contains("VALUES (?"), "last chunk single tuple");
        // 每个元素分摊后的受影响行数应为 1（MySQL 返回值/行数）
        assertEquals(1, result[0], "per-row affected should be 1");
    }

    /**
     * JDBC_BATCH 模式：保留 batchUpdate 语义，每条记录一个受影响计数。
     */
    private static void verifyJdbcBatchExecution() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestBatchSupport support = new TestBatchSupport(jdbc, multiValueRuntime());

        List<BatchVerifyEntity> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BatchVerifyEntity e = new BatchVerifyEntity();
            e.setName("u" + i);
            e.setAge(i);
            e.setEmail("u" + i + "@x.com");
            list.add(e);
        }

        int[] result = support.batchInsert(list, false, BatchMode.JDBC_BATCH);
        assertEquals(3, result.length, "result length mismatch");
        assertEquals(1, jdbc.batchSqls.size(), "single batch statement expected");
        assertEquals(3, jdbc.batchArgsList.get(0).size(), "batch args should cover all rows");
        assertEquals(1, result[0], "per-row affected should be 1");
    }

    /**
     * 2 参 batchInsert 应遵循运行时默认模式（MULTI_VALUE）。
     */
    private static void verifyRuntimeDefaultMode() {
        RecordingJdbcTemplate jdbc = new RecordingJdbcTemplate();
        TestBatchSupport support = new TestBatchSupport(jdbc, multiValueRuntime());

        BatchVerifyEntity e = new BatchVerifyEntity();
        e.setName("u");
        e.setAge(1);
        e.setEmail("u@x.com");
        int[] result = support.batchInsert(Arrays.asList(e), false);
        assertEquals(1, result.length, "result length mismatch");
        assertEquals(1, jdbc.updateSqls.size(), "runtime default mode should be MULTI_VALUE");
    }

    private static TinyJdbcRuntime runtime() {
        return new TinyJdbcRuntime(false, "verify", null, false, null, null, null);
    }

    private static TinyJdbcRuntime multiValueRuntime() {
        return new TinyJdbcRuntime(false, "verify", null, false, null, null, null, BatchMode.MULTI_VALUE, 2);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertThrows(Runnable action, String messageFragment) {
        try {
            action.run();
        } catch (TinyJdbcException e) {
            if (!e.getMessage().contains(messageFragment)) {
                throw new IllegalStateException("expected exception message to contain '" + messageFragment
                        + "' but was '" + e.getMessage() + "'");
            }
            return;
        }
        throw new IllegalStateException("expected TinyJdbcException with message containing '" + messageFragment + "'");
    }

    /**
     * 记录 update / batchUpdate 调用而不真正访问数据库的测试模板。
     */
    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> updateSqls = new ArrayList<>();
        private final List<String> batchSqls = new ArrayList<>();
        private final List<List<Object[]>> batchArgsList = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {
            this.updateSqls.add(sql);
            return tupleCount(sql, args);
        }

        @Override
        public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
            this.batchSqls.add(sql);
            this.batchArgsList.add(new ArrayList<>(batchArgs));
            int[] result = new int[batchArgs.size()];
            Arrays.fill(result, 1);
            return result;
        }

        /**
         * 依据 SQL 中首个 VALUES 元组的占位符数反推行数，作为受影响行数（MySQL 每行返回 1）。
         */
        private int tupleCount(String sql, Object[] args) {
            int idx = sql == null ? -1 : sql.indexOf("VALUES");
            if (idx < 0 || args == null) {
                return 0;
            }
            int start = sql.indexOf('(', idx);
            int end = sql.indexOf(')', start);
            if (start < 0 || end < 0) {
                return 0;
            }
            int cols = 0;
            for (int i = start; i < end; i++) {
                if (sql.charAt(i) == '?') {
                    cols++;
                }
            }
            return cols == 0 ? 0 : args.length / cols;
        }
    }

    /**
     * 提供 AbstractSqlSupport 运行所需依赖的测试实现。
     */
    private static class TestBatchSupport extends AbstractSqlSupport<BatchVerifyEntity, Long> {
        private final JdbcTemplate jdbcTemplate;
        private final TinyJdbcRuntime tinyJdbcRuntime;

        private TestBatchSupport(JdbcTemplate jdbcTemplate, TinyJdbcRuntime tinyJdbcRuntime) {
            this.jdbcTemplate = jdbcTemplate;
            this.tinyJdbcRuntime = tinyJdbcRuntime;
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
            return new ArrayList<>();
        }

        @Override
        protected TinyJdbcRuntime getTinyJdbcRuntime() {
            return this.tinyJdbcRuntime;
        }

        @Override
        protected NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
            return null;
        }
    }
}
