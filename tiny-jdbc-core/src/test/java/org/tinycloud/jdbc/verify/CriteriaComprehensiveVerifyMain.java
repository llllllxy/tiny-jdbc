package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.criteria.query.LambdaQueryCriteria;
import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.support.SqlAssembler;
import org.tinycloud.jdbc.support.SqlProvider;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

/**
 * Criteria 模块综合回归：覆盖 4 类构造器、条件方法族、SQL 片段生成、参数顺序、
 * 状态机与边界校验。与 AndOr / GroupByHaving / Robustness 等专项测试互补。
 */
public class CriteriaComprehensiveVerifyMain {

    private static final String COLUMNS = "id,create_user_id,create_time,update_user_id,update_time";

    @Test
    public void testAll() {
        // 1. 基础条件族：字符串版 / Lambda 版逐方法对齐
        verifyStringComparisonConditions();
        verifyLambdaComparisonConditions();
        verifyStringLikeConditions();
        verifyLambdaLikeConditions();
        verifyStringNullAndBetween();
        verifyLambdaNullAndBetween();

        // 2. 集合 / 子查询 / 原 SQL
        verifyStringInCollectionAndVarargs();
        verifyLambdaInCollectionAndVarargs();
        verifyStringExistsAndApply();
        verifyLambdaExistsAndApply();

        // 3. SELECT 片段生成
        verifyQueryCriteriaSqlFragments();
        verifyLambdaQueryCriteriaSqlFragments();
        verifyGroupByHavingAndCountSql();

        // 4. UPDATE 片段生成
        verifyUpdateCriteriaSqlFragments();
        verifyLambdaUpdateCriteriaSqlFragments();

        // 5. 状态机与边界
        verifyWhetherFalseAndNestedState();
        verifyParameterOrderAcrossAllClauseTypes();
        verifyIllegalInputRejected();
        verifyConditionParameterCopies();

        System.out.println("CriteriaComprehensiveVerifyMain passed.");
    }

    // ---------------- 基础条件族 ----------------

    private static void verifyStringComparisonConditions() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .lt("id", 1L).lte("create_user_id", 2L)
                .gt("update_user_id", 3L).gte("id", 4L)
                .eq("create_user_id", 5L).notEq("update_user_id", 6L);
        assertEquals(" WHERE id < ? AND create_user_id <= ? AND update_user_id > ? "
                + "AND id >= ? AND create_user_id = ? AND update_user_id <> ?", c.whereConditions());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), c.getConditionParameters());
    }

    private static void verifyLambdaComparisonConditions() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .lt(VerifyDemoEntity::getId, 1L).lte(VerifyDemoEntity::getCreateUserId, 2L)
                .gt(VerifyDemoEntity::getUpdateUserId, 3L).gte(VerifyDemoEntity::getId, 4L)
                .eq(VerifyDemoEntity::getCreateUserId, 5L).notEq(VerifyDemoEntity::getUpdateUserId, 6L);
        assertEquals(" WHERE id < ? AND create_user_id <= ? AND update_user_id > ? "
                + "AND id >= ? AND create_user_id = ? AND update_user_id <> ?", c.whereConditions());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), c.getConditionParameters());
    }

    private static void verifyStringLikeConditions() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .like("create_user_id", "a").notLike("create_user_id", "b")
                .leftLike("create_user_id", "c").notLeftLike("create_user_id", "d")
                .rightLike("create_user_id", "e").notRightLike("create_user_id", "f");
        assertEquals(" WHERE create_user_id LIKE ? AND create_user_id NOT LIKE ? "
                + "AND create_user_id LIKE ? AND create_user_id NOT LIKE ? "
                + "AND create_user_id LIKE ? AND create_user_id NOT LIKE ?", c.whereConditions());
        assertEquals(Arrays.asList("%a%", "%b%", "%c", "%d", "e%", "f%"), c.getConditionParameters());
    }

    private static void verifyLambdaLikeConditions() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .like(VerifyDemoEntity::getCreateUserId, "a").notLike(VerifyDemoEntity::getCreateUserId, "b")
                .leftLike(VerifyDemoEntity::getCreateUserId, "c").notLeftLike(VerifyDemoEntity::getCreateUserId, "d")
                .rightLike(VerifyDemoEntity::getCreateUserId, "e").notRightLike(VerifyDemoEntity::getCreateUserId, "f");
        assertEquals(" WHERE create_user_id LIKE ? AND create_user_id NOT LIKE ? "
                + "AND create_user_id LIKE ? AND create_user_id NOT LIKE ? "
                + "AND create_user_id LIKE ? AND create_user_id NOT LIKE ?", c.whereConditions());
        assertEquals(Arrays.asList("%a%", "%b%", "%c", "%d", "e%", "f%"), c.getConditionParameters());
    }

    private static void verifyStringNullAndBetween() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .isNull("create_user_id").isNotNull("update_user_id")
                .between("id", 10L, 20L).notBetween("create_user_id", 30L, 40L);
        assertEquals(" WHERE create_user_id IS NULL AND update_user_id IS NOT NULL "
                + "AND (id BETWEEN ? AND ?) AND (create_user_id NOT BETWEEN ? AND ?)", c.whereConditions());
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L), c.getConditionParameters());
    }

    private static void verifyLambdaNullAndBetween() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .isNull(VerifyDemoEntity::getCreateUserId).isNotNull(VerifyDemoEntity::getUpdateUserId)
                .between(VerifyDemoEntity::getId, 10L, 20L)
                .notBetween(VerifyDemoEntity::getCreateUserId, 30L, 40L);
        assertEquals(" WHERE create_user_id IS NULL AND update_user_id IS NOT NULL "
                + "AND (id BETWEEN ? AND ?) AND (create_user_id NOT BETWEEN ? AND ?)", c.whereConditions());
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L), c.getConditionParameters());
    }

    // ---------------- 集合 / 子查询 / 原 SQL ----------------

    private static void verifyStringInCollectionAndVarargs() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .in("id", Arrays.asList(1L, 2L))
                .notIn("update_user_id", Arrays.asList(3L, 4L))
                .in("create_user_id", 5L, 6L)
                .notIn("id", 7L);
        assertEquals(" WHERE id IN (?, ?) AND update_user_id NOT IN (?, ?) "
                + "AND create_user_id IN (?, ?) AND id NOT IN (?)", c.whereConditions());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L), c.getConditionParameters());
    }

    private static void verifyLambdaInCollectionAndVarargs() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .in(VerifyDemoEntity::getId, Arrays.asList(1L, 2L))
                .notIn(VerifyDemoEntity::getUpdateUserId, Arrays.asList(3L, 4L))
                .in(VerifyDemoEntity::getCreateUserId, 5L, 6L)
                .notIn(VerifyDemoEntity::getId, 7L);
        assertEquals(" WHERE id IN (?, ?) AND update_user_id NOT IN (?, ?) "
                + "AND create_user_id IN (?, ?) AND id NOT IN (?)", c.whereConditions());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L), c.getConditionParameters());
    }

    private static void verifyStringExistsAndApply() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .exists("select 1 from t_other o where o.id = t_verify_demo.id")
                .notExists("select 1 from t_other o where o.pid = ?", 8L)
                .apply("id = ?", 7L);
        assertEquals(" WHERE EXISTS (select 1 from t_other o where o.id = t_verify_demo.id) "
                + "AND NOT EXISTS (select 1 from t_other o where o.pid = ?) AND id = ?", c.whereConditions());
        assertEquals(Arrays.asList(8L, 7L), c.getConditionParameters());
    }

    private static void verifyLambdaExistsAndApply() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .exists("select 1 from t_other o where o.id = t_verify_demo.id")
                .notExists("select 1 from t_other o where o.pid = ?", 8L)
                .apply("id = ?", 7L);
        assertEquals(" WHERE EXISTS (select 1 from t_other o where o.id = t_verify_demo.id) "
                + "AND NOT EXISTS (select 1 from t_other o where o.pid = ?) AND id = ?", c.whereConditions());
        assertEquals(Arrays.asList(8L, 7L), c.getConditionParameters());
    }

    // ---------------- SELECT 片段 ----------------

    private static void verifyQueryCriteriaSqlFragments() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .select("id", "create_user_id")
                .eq("id", 1L)
                .orderBy("id", false).orderByDesc("create_user_id")
                .last("LIMIT 5");
        SqlProvider p = SqlAssembler.buildSelectCriteriaSql(c, VerifyDemoEntity.class);
        assertEquals("SELECT id,create_user_id FROM t_verify_demo WHERE id = ? ORDER BY id,create_user_id DESC LIMIT 5", p.getSql());
        assertEquals(Arrays.asList(1L), p.getParameters());
        assertEquals("id,create_user_id", c.selectSql());
    }

    private static void verifyLambdaQueryCriteriaSqlFragments() {
        LambdaQueryCriteria<VerifyDemoEntity> c = new LambdaQueryCriteria<VerifyDemoEntity>()
                .select(VerifyDemoEntity::getId, VerifyDemoEntity::getCreateUserId)
                .eq(VerifyDemoEntity::getId, 1L)
                .orderBy(VerifyDemoEntity::getId, false)
                .orderByDesc(VerifyDemoEntity::getCreateUserId);
        SqlProvider p = SqlAssembler.buildSelectLambdaCriteriaSql(c, VerifyDemoEntity.class);
        assertEquals("SELECT id,create_user_id FROM t_verify_demo WHERE id = ? ORDER BY id,create_user_id DESC", p.getSql());
        assertEquals(Arrays.asList(1L), p.getParameters());
    }

    private static void verifyGroupByHavingAndCountSql() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .selectExpr("count(*) as cnt").select("create_user_id")
                .eq("id", 1L)
                .groupBy("create_user_id")
                .having("count(*) > ?", 5)
                .orderByDesc("create_user_id");

        assertTrue(c.hasGroupByOrHaving(), "group by / having should be detected");
        assertEquals(" WHERE id = ? GROUP BY create_user_id HAVING count(*) > ? ORDER BY create_user_id DESC", c.whereSql());
        assertEquals(" WHERE id = ? GROUP BY create_user_id HAVING count(*) > ?", c.whereGroupByHavingSql());

        SqlProvider count = SqlAssembler.buildSelectCountCriteriaSql(c, VerifyDemoEntity.class);
        assertEquals("SELECT COUNT(*) FROM (SELECT 1 FROM t_verify_demo WHERE id = ? "
                + "GROUP BY create_user_id HAVING count(*) > ?) tiny_jdbc_count", count.getSql());
        assertEquals(Arrays.asList(1L, 5), count.getParameters());
    }

    // ---------------- UPDATE 片段 ----------------

    private static void verifyUpdateCriteriaSqlFragments() {
        UpdateCriteria<VerifyDemoEntity> c = new UpdateCriteria<VerifyDemoEntity>()
                .set("create_user_id", 100L)
                .setIncrement("update_user_id", 3)
                .setDecrement("id", BigDecimal.valueOf(2))
                .setField("update_time", "create_time")
                .eq("id", 1L);
        assertEquals("create_user_id = ?,update_user_id = update_user_id + 3,"
                + "id = id - 2,update_time = create_time", c.updateSql());
        assertEquals(" WHERE id = ?", c.whereSql());
        assertEquals(Arrays.asList(100L, 1L), c.getParameters());
        assertEquals(Collections.singletonList(1L), c.getConditionParameters());
        assertTrue(c.hasUpdateColumn("create_user_id"), "hasUpdateColumn should see set column");
        assertTrue(c.hasUpdateColumn("id"), "hasUpdateColumn should see increment column");
        assertTrue(!c.hasUpdateColumn("not_a_column"), "hasUpdateColumn should reject unknown column");
    }

    private static void verifyLambdaUpdateCriteriaSqlFragments() {
        LambdaUpdateCriteria<VerifyDemoEntity> c = new LambdaUpdateCriteria<VerifyDemoEntity>()
                .set(VerifyDemoEntity::getCreateUserId, 100L)
                .setIncrement(VerifyDemoEntity::getUpdateUserId, 3)
                .setDecrement(VerifyDemoEntity::getId, 2)
                .setField(VerifyDemoEntity::getUpdateTime, VerifyDemoEntity::getCreateTime)
                .eq(VerifyDemoEntity::getId, 1L);
        assertEquals("create_user_id = ?,update_user_id = update_user_id + 3,"
                + "id = id - 2,update_time = create_time", c.updateSql());
        assertEquals(" WHERE id = ?", c.whereSql());
        assertEquals(Arrays.asList(100L, 1L), c.getParameters());
    }

    // ---------------- 状态机与边界 ----------------

    private static void verifyWhetherFalseAndNestedState() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>()
                .eq(true, "id", 1L)
                .eq(false, "create_user_id", 2L)
                .or()
                .and(group -> group.eq("update_user_id", 3L).eq(false, "id", 4L))
                .or(group -> group.eq("create_user_id", 5L))
                .eq("id", 6L);
        assertEquals(" WHERE id = ? OR (update_user_id = ?) OR (create_user_id = ?) AND id = ?", c.whereConditions());
        assertEquals(Arrays.asList(1L, 3L, 5L, 6L), c.getConditionParameters());
    }

    private static void verifyParameterOrderAcrossAllClauseTypes() {
        QueryCriteria<VerifyDemoEntity> query = new QueryCriteria<VerifyDemoEntity>()
                .eq("id", 1L)
                .having("count(*) > ?", 5)
                .groupBy("create_user_id");
        // 查询侧：WHERE → HAVING
        assertEquals(Arrays.asList(1L, 5), query.getParameters());
        assertEquals(Arrays.asList(1L, 5), query.getConditionParameters());

        UpdateCriteria<VerifyDemoEntity> update = new UpdateCriteria<VerifyDemoEntity>()
                .set("create_user_id", 100L)
                .eq("id", 1L);
        // 更新侧：SET 值 → WHERE
        assertEquals(Arrays.asList(100L, 1L), update.getParameters());
        assertEquals(Collections.singletonList(1L), update.getConditionParameters());
    }

    private static void verifyIllegalInputRejected() {
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().eq("id;drop", 1L),
                "illegal column ref should be rejected");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().in("id", Collections.emptyList()),
                "empty IN list should be rejected");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("SELECT 1"),
                "unsafe last clause should be rejected");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().apply("1=1 OR 1=1"),
                "top-level OR in apply should be rejected");
    }

    private static void verifyConditionParameterCopies() {
        QueryCriteria<VerifyDemoEntity> c = new QueryCriteria<VerifyDemoEntity>().eq("id", 1L);
        java.util.List<Object> first = c.getConditionParameters();
        first.add("changed");
        assertEquals(Arrays.asList(1L), c.getConditionParameters());

        java.util.List<Object> all = c.getParameters();
        all.add("changed");
        assertEquals(Arrays.asList(1L), c.getParameters());
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (TinyJdbcException e) {
            return;
        }
        throw new IllegalStateException("expected TinyJdbcException: " + message);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException("expected=" + expected + ", actual=" + actual);
        }
    }
}
