package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.util.Arrays;

/**
 * 验证 criteria 扩展能力：
 * <ul>
 *   <li>{@code exists/notExists} 子查询条件</li>
 *   <li>{@code apply} 原始 SQL 片段条件</li>
 *   <li>{@code in/notIn} 可变参数重载</li>
 *   <li>{@code setField} 字段对字段赋值</li>
 * </ul>
 */
public class CriteriaExtendVerifyMain {

    /** 实体全部有效列，用于 SELECT 列预期（字段声明顺序）。 */
    private static final String COLUMNS = "id,create_user_id,create_time,update_user_id,update_time";

    @Test
    public void testAll() {
        verifyStringInVarargs();
        verifyLambdaInVarargs();
        verifyStringExistsApply();
        verifyLambdaExistsApply();
        verifyUpdateSetField();
        verifySelectExpr();
        verifyLambdaSelectExpr();
        System.out.println("CriteriaExtendVerifyMain passed.");
    }

    /**
     * 字符串版 in 可变参数：生成 {@code IN (?, ?)} 并把值按序绑定。
     */
    private static void verifyStringInVarargs() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .in("create_user_id", 100L, 200L);
        SqlProvider provider = SqlAssembler.buildSelectCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT " + COLUMNS + " FROM t_verify_demo WHERE create_user_id IN (?, ?)", provider.getSql());
        assertEquals(Arrays.asList(100L, 200L), provider.getParameters());
    }

    /**
     * Lambda 版 in 可变参数。
     */
    private static void verifyLambdaInVarargs() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .in(VerifyDemoEntity::getCreateUserId, 100L, 200L);
        SqlProvider provider = SqlAssembler.buildSelectLambdaCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT " + COLUMNS + " FROM t_verify_demo WHERE create_user_id IN (?, ?)", provider.getSql());
        assertEquals(Arrays.asList(100L, 200L), provider.getParameters());
    }

    /**
     * 字符串版 exists + apply：子查询原样包裹，apply 参数化。
     */
    private static void verifyStringExistsApply() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .exists("select 1 from t_order o where o.user_id = create_user_id")
                .apply("date(create_time) = ?", "2026-01-01");

        SqlProvider provider = SqlAssembler.buildSelectCriteriaSql(criteria, VerifyDemoEntity.class);
        assertTrue(provider.getSql().contains("EXISTS (select 1 from t_order o where o.user_id = create_user_id)"));
        assertTrue(provider.getSql().contains("date(create_time) = ?"));
        assertEquals(Arrays.asList("2026-01-01"), provider.getParameters());
    }

    /**
     * Lambda 版 exists + apply。
     */
    private static void verifyLambdaExistsApply() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .exists("select 1 from t_order o where o.user_id = create_user_id")
                .apply("date(create_time) = ?", "2026-01-01");

        SqlProvider provider = SqlAssembler.buildSelectLambdaCriteriaSql(criteria, VerifyDemoEntity.class);
        assertTrue(provider.getSql().contains("EXISTS (select 1 from t_order o where o.user_id = create_user_id)"));
        assertTrue(provider.getSql().contains("date(create_time) = ?"));
        assertEquals(Arrays.asList("2026-01-01"), provider.getParameters());
    }

    /**
     * setField 字段对字段赋值：字符串版与 Lambda 版都应生成 {@code create_time = update_time}。
     */
    private static void verifyUpdateSetField() {
        UpdateCriteria<VerifyDemoEntity> updateCriteria = new UpdateCriteria<VerifyDemoEntity>()
                .setField("create_time", "update_time");
        assertEquals("create_time = update_time", updateCriteria.updateSql());

        LambdaUpdateCriteria<VerifyDemoEntity> lambdaUpdateCriteria = new LambdaUpdateCriteria<VerifyDemoEntity>()
                .setField(VerifyDemoEntity::getCreateTime, VerifyDemoEntity::getUpdateTime);
        assertEquals("create_time = update_time", lambdaUpdateCriteria.updateSql());
    }

    /**
     * selectExpr 聚合列 + groupBy + having：验证聚合列能进 SELECT，且顺序正确。
     */
    private static void verifySelectExpr() {
        QueryCriteria<VerifyDemoEntity> criteria = new QueryCriteria<VerifyDemoEntity>()
                .select("create_user_id")
                .selectExpr("count(*) as cnt")
                .groupBy("create_user_id")
                .having("count(*) > ?", 5);
        SqlProvider provider = SqlAssembler.buildSelectCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT create_user_id,count(*) as cnt FROM t_verify_demo "
                + "GROUP BY create_user_id HAVING count(*) > ?", provider.getSql());
        assertEquals(Arrays.asList(5), provider.getParameters());
    }

    /**
     * Lambda 版：selectExpr + groupBy + having。
     */
    private static void verifyLambdaSelectExpr() {
        LambdaQueryCriteria<VerifyDemoEntity> criteria = new LambdaQueryCriteria<VerifyDemoEntity>()
                .select(VerifyDemoEntity::getCreateUserId)
                .selectExpr("count(*) as cnt")
                .groupBy(VerifyDemoEntity::getCreateUserId)
                .having("count(*) > ?", 5);
        SqlProvider provider = SqlAssembler.buildSelectLambdaCriteriaSql(criteria, VerifyDemoEntity.class);
        assertEquals("SELECT create_user_id,count(*) as cnt FROM t_verify_demo "
                + "GROUP BY create_user_id HAVING count(*) > ?", provider.getSql());
        assertEquals(Arrays.asList(5), provider.getParameters());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new IllegalStateException("assertion failed");
        }
    }
}
