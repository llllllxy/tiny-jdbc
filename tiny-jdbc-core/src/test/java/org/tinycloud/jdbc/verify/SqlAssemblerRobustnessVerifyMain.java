package org.tinycloud.jdbc.verify;

import org.junit.Test;

import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.BatchInsertSql;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * SQL 组装器健壮性回归测试：异构批量、批量 null 元素、byIds 空/可变列表。
 */
public class SqlAssemblerRobustnessVerifyMain {

    @Test
    public void testAll() {
        verifyHeterogeneousBatchRejected();
        verifyNullBatchElementRejected();
        verifyNullBatchCollectionRejected();
        verifyByIdsNullOrEmptyRejected();
        verifyByIdsDefensiveCopy();
        System.out.println("SqlAssemblerRobustnessVerifyMain passed.");
    }

    /** P0 + 跨表误写：不同实体类型不能混进同一批量插入。 */
    private static void verifyHeterogeneousBatchRejected() {
        List<Object> mixed = Arrays.asList(new VerifyDemoEntity(), new OtherTableEntity());
        assertThrows(() -> SqlAssembler.buildBatchInsert(mixed, false, null, null),
                "heterogeneous batch entities should be rejected");
    }

    /** P2：批量元素为 null 应给出框架异常而非 NPE。 */
    private static void verifyNullBatchElementRejected() {
        List<VerifyDemoEntity> withNull = new ArrayList<>();
        withNull.add(new VerifyDemoEntity());
        withNull.add(null);
        assertThrows(() -> SqlAssembler.buildBatchInsert(withNull, false, null, null),
                "null batch element should be rejected");
    }

    /** 空集合：保留原有明确异常。 */
    private static void verifyNullBatchCollectionRejected() {
        assertThrows(() -> SqlAssembler.buildBatchInsert(Collections.emptyList(), false, null, null),
                "empty batch collection should be rejected");
    }

    /** P1 byIds：空/ null 列表应给出框架异常，而不是 NPE 或非法 IN ()。 */
    private static void verifyByIdsNullOrEmptyRejected() {
        assertThrows(() -> SqlAssembler.buildSelectByIdsSql(VerifyDemoEntity.class, Collections.emptyList()),
                "empty selectByIds ids should be rejected");
        assertThrows(() -> SqlAssembler.buildSelectByIdsSql(VerifyDemoEntity.class, null),
                "null selectByIds ids should be rejected");
        assertThrows(() -> SqlAssembler.buildDeleteByIdsSql(VerifyDemoEntity.class, Collections.emptyList()),
                "empty deleteByIds ids should be rejected");
        assertThrows(() -> SqlAssembler.buildDeleteByIdsSql(VerifyDemoEntity.class, null),
                "null deleteByIds ids should be rejected");
    }

    /** P1 byIds：防御性拷贝，构建后修改原列表不应影响 SqlProvider 参数。 */
    private static void verifyByIdsDefensiveCopy() {
        List<Object> ids = new ArrayList<>(Arrays.asList(1L, 2L));
        SqlProvider provider = SqlAssembler.buildSelectByIdsSql(VerifyDemoEntity.class, ids);
        assertEquals("SELECT id,create_user_id,create_time,update_user_id,update_time FROM t_verify_demo WHERE id IN (?,?)",
                provider.getSql(), "selectByIds SQL mismatch");
        assertEquals(Arrays.asList(1L, 2L), provider.getParameters(), "selectByIds initial params mismatch");

        // 调用方后续清空原列表，provider 参数应保持
        ids.clear();
        assertEquals(Arrays.asList(1L, 2L), provider.getParameters(),
                "selectByIds params should be a defensive copy");

        List<Object> deleteIds = new ArrayList<>(Arrays.asList(3L, 4L));
        SqlProvider deleteProvider = SqlAssembler.buildDeleteByIdsSql(VerifyDemoEntity.class, deleteIds);
        assertEquals("DELETE FROM t_verify_demo WHERE id IN (?,?)", deleteProvider.getSql(),
                "deleteByIds SQL mismatch");
        deleteIds.clear();
        assertEquals(Arrays.asList(3L, 4L), deleteProvider.getParameters(),
                "deleteByIds params should be a defensive copy");
    }

    private static void assertThrows(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (TinyJdbcException e) {
            return;
        } catch (Throwable e) {
            throw new IllegalStateException(message + ", but got unexpected " + e.getClass().getName() + ": " + e.getMessage(), e);
        }
        throw new IllegalStateException(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    /** 另一个实体类（不同表），用于异构批量校验。 */
    @Table("t_other")
    public static class OtherTableEntity {
        @Id(idType = IdType.AUTO_INCREMENT)
        private Long id;
        private String name;
    }
}
