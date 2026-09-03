package org.tinycloud.jdbc.verify;

import org.junit.Test;

import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.Arrays;

/**
 * 验证统计总数 SQL 只包含条件，不携带 {@code ORDER BY} 或 {@code last()} 尾片段。
 *
 * <p>回归场景：{@code selectCount} 之前复用 {@link QueryCriteria#whereSql()}，
 * 导致生成 {@code SELECT COUNT(*) ... ORDER BY ...} 或带 {@code FOR UPDATE} 等
 * 在 PostgreSQL 等数据库上非法 / 语义错误的聚合查询。</p>
 */
public class SqlAssemblerCountVerifyMain {

    @Test
    public void testAll() {
        verifyQueryCriteriaCountStripsOrderAndLast();
        verifyLambdaCriteriaCountStripsOrder();
        verifyWhereConditionsStillLetsWhereSqlKeepOrderAndLast();
        System.out.println("SqlAssemblerCountVerifyMain passed.");
    }

    /**
     * QueryCriteria 版：count 应剔除排序与 last，参数只保留条件参数。
     */
    private static void verifyQueryCriteriaCountStripsOrderAndLast() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .orderBy("create_user_id")
                .last("FOR UPDATE");

        // 条件部分（whereConditions）应只含 WHERE，不含 ORDER BY / last
        assertEquals(" WHERE create_user_id = ?", criteria.whereConditions(), "whereConditions should only contain predicates");

        SqlProvider provider = SqlAssembler.buildSelectCountCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT COUNT(*) FROM t_verify_demo WHERE create_user_id = ?", provider.getSql(),
                "count SQL should not contain ORDER BY or last");
        assertEquals(Arrays.asList(100L), provider.getParameters(), "count params should only contain condition params");
    }

    /**
     * LambdaCriteria 版：同样剔除排序。
     */
    private static void verifyLambdaCriteriaCountStripsOrder() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .eq(VerifyDemoEntity::getCreateUserId, 100L)
                .orderBy(VerifyDemoEntity::getCreateUserId);

        SqlProvider provider = SqlAssembler.buildSelectCountLambdaCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT COUNT(*) FROM t_verify_demo WHERE create_user_id = ?", provider.getSql(),
                "lambda count SQL should not contain ORDER BY");
        assertEquals(Arrays.asList(100L), provider.getParameters(), "lambda count params mismatch");
    }

    /**
     * whereSql 仍保留 ORDER BY 与 last（用于真正的列表查询），保持向后兼容。
     */
    private static void verifyWhereConditionsStillLetsWhereSqlKeepOrderAndLast() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .orderBy("create_user_id")
                .last("FOR UPDATE");

        assertEquals(" WHERE create_user_id = ? ORDER BY create_user_id FOR UPDATE", criteria.whereSql(),
                "whereSql should still keep ORDER BY and last for list queries");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }
}
