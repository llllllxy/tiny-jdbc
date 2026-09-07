package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.Arrays;

/**
 * 验证 criteria 新增的 {@code groupBy} / {@code having} 能正确拼进 SELECT SQL：
 * 位置为 {@code WHERE ... GROUP BY ... HAVING ... ORDER BY ...}，HAVING 参数按顺序绑定。
 */
public class GroupByHavingVerifyMain {

    /** 实体全部有效列，用于 SELECT 列预期（字段声明顺序）。 */
    private static final String COLUMNS = "id,create_user_id,create_time,update_user_id,update_time";

    @Test
    public void testAll() {
        verifyQueryCriteriaGroupByHaving();
        verifyLambdaCriteriaGroupByHaving();
        System.out.println("GroupByHavingVerifyMain passed.");
    }

    private static void verifyQueryCriteriaGroupByHaving() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .eq("create_user_id", 100L)
                .groupBy("create_user_id")
                .having("count(*) > ?", 5);

        SqlProvider provider = SqlAssembler.buildSelectCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT " + COLUMNS + " FROM t_verify_demo "
                        + "WHERE create_user_id = ? GROUP BY create_user_id HAVING count(*) > ?",
                provider.getSql());
        assertEquals(Arrays.asList(100L, 5), provider.getParameters());
    }

    private static void verifyLambdaCriteriaGroupByHaving() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .groupBy(VerifyDemoEntity::getCreateUserId)
                .having("count(*) > ?", 5)
                .orderByDesc(VerifyDemoEntity::getCreateUserId);

        SqlProvider provider = SqlAssembler.buildSelectLambdaCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT " + COLUMNS + " FROM t_verify_demo "
                        + "GROUP BY create_user_id HAVING count(*) > ? ORDER BY create_user_id DESC",
                provider.getSql());
        assertEquals(Arrays.asList(5), provider.getParameters());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("expected=" + expected + ", actual=" + actual);
        }
    }
}
