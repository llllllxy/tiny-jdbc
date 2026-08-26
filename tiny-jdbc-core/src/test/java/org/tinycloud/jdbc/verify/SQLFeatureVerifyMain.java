package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.sql.FuncBuilder;
import org.tinycloud.jdbc.sql.SQL;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * 阶段3 功能对齐测试：主表别名、多行插入、函数补齐 + FuncExpr 嵌套、CASE WHEN 三参、Join.andIfAbsent。
 */
public class SQLFeatureVerifyMain {

    // ---------- 1. 主表别名 ----------

    @Test
    public void testSelectTableAlias() {
        SQL<?> sq = SQL.table("tb_user", "u").select("u.name").where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u WHERE u.is_active = ?", sq.toSql());
        assertEquals(1, sq.getParameters().size());
        assertEquals(1, sq.getParameters().get(0));
    }

    @Test
    public void testUpdateTableAlias() {
        SQL<?> sq = SQL.table("tb_user", "u").update().set("u.email", "x@y.com").where(w -> w.eq("u.id", 1));
        assertEquals("UPDATE tb_user u SET u.email = ? WHERE u.id = ?", sq.toSql());
        assertEquals(2, sq.getParameters().size());
        assertEquals("x@y.com", sq.getParameters().get(0));
        assertEquals(1, sq.getParameters().get(1));
    }

    @Test
    public void testDeleteTableAlias() {
        SQL<?> sq = SQL.table("tb_user", "u").delete().where(w -> w.eq("u.id", 1));
        assertEquals("DELETE FROM tb_user u WHERE u.id = ?", sq.toSql());
    }

    // ---------- 2. 多行插入 ----------

    @Test
    public void testMultiRowInsert() {
        SQL<?> sq = SQL.table("tb_user").insert("id", "name").values(1, "a").values(2, "b");
        assertEquals("INSERT INTO tb_user (id, name) VALUES (?, ?), (?, ?)", sq.toSql());
        assertEquals(4, sq.getParameters().size());
        assertEquals(1, sq.getParameters().get(0));
        assertEquals("a", sq.getParameters().get(1));
        assertEquals(2, sq.getParameters().get(2));
        assertEquals("b", sq.getParameters().get(3));
    }

    @Test
    public void testMultiRowInsertIgnore() {
        SQL<?> sq = SQL.table("tb_user").insertIgnoreInto("id", "name").values(1, "a").values(2, "b");
        assertEquals("INSERT IGNORE INTO tb_user (id, name) VALUES (?, ?), (?, ?)", sq.toSql());
    }

    // ---------- 3. 函数补齐 + FuncExpr 嵌套 ----------

    @Test
    public void testYearNowNesting() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.year(FuncBuilder.now()).as("y"));
        assertEquals("SELECT YEAR(NOW()) AS y FROM t", sq.toSql());
        assertEquals(0, sq.getParameters().size());
    }

    @Test
    public void testNestedTrimLower() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.trim(FuncBuilder.lower(FuncBuilder.col("name"))).as("n"));
        assertEquals("SELECT TRIM(LOWER(name)) AS n FROM t", sq.toSql());
    }

    @Test
    public void testGenericFunc() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.func("COALESCE", FuncBuilder.col("a"), FuncBuilder.lit("x")).as("c"));
        assertEquals("SELECT COALESCE(a, ?) AS c FROM t", sq.toSql());
        assertEquals("x", sq.getParameters().get(0));
    }

    @Test
    public void testDateSub() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.dateSub("create_time", 7).as("d"));
        assertEquals("SELECT DATE_SUB(create_time, INTERVAL 7 DAY) AS d FROM t", sq.toSql());
    }

    @Test
    public void testJsonExtract() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.jsonExtract("data", "$.name").as("n"));
        assertEquals("SELECT JSON_EXTRACT(data, ?) AS n FROM t", sq.toSql());
        assertEquals("$.name", sq.getParameters().get(0));
    }

    @Test
    public void testIfThreeArg() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder._if("1=1", FuncBuilder.lit("a"), FuncBuilder.lit("b")).as("x"));
        assertEquals("SELECT IF(1=1, ?, ?) AS x FROM t", sq.toSql());
        assertEquals("a", sq.getParameters().get(0));
        assertEquals("b", sq.getParameters().get(1));
    }

    // ---------- 4. CASE WHEN 三参 + Join.andIfAbsent ----------

    @Test
    public void testCaseWhenShortcut() {
        SQL<?> sq = SQL.table("t").select(
                FuncBuilder.caseWhen("salary > 10000", FuncBuilder.lit("high"), FuncBuilder.lit("normal")).as("level"));
        assertEquals("SELECT CASE WHEN salary > 10000 THEN ? ELSE ? END AS level FROM t", sq.toSql());
        assertEquals("high", sq.getParameters().get(0));
        assertEquals("normal", sq.getParameters().get(1));
    }

    @Test
    public void testJoinAndIfAbsentSkipWhenNull() {
        SQL<?> sq = SQL.table("tb_user", "u").select("u.name")
                .innerJoin("tb_role", "r", on -> on.on("u.role_id", "r.id").andIfAbsent("r.status", "=", null))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON u.role_id = r.id WHERE u.is_active = ?", sq.toSql());
    }

    @Test
    public void testJoinAndIfAbsentWhenPresent() {
        SQL<?> sq = SQL.table("tb_user", "u").select("u.name")
                .innerJoin("tb_role", "r", on -> on.on("u.role_id", "r.id").andIfAbsent("r.status", "=", 1))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON u.role_id = r.id AND r.status = ? WHERE u.is_active = ?", sq.toSql());
        assertEquals(1, sq.getParameters().get(0));
        assertEquals(1, sq.getParameters().get(1));
    }

    // ---------- 5. 插入的 Lambda 重载（insertInto / insertIgnoreInto / replaceInto + TypeFunction） ----------

    @Test
    public void testInsertIntoWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .insertInto(VerifyChildEntity::getChildName)
                .values("张三");
        assertEquals("INSERT INTO t_verify_child (child_name_col) VALUES (?)", sq.toSql());
        assertEquals("张三", sq.getParameters().get(0));
    }

    @Test
    public void testInsertIgnoreIntoWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .insertIgnoreInto(VerifyChildEntity::getChildName)
                .values("李四");
        assertEquals("INSERT IGNORE INTO t_verify_child (child_name_col) VALUES (?)", sq.toSql());
    }

    @Test
    public void testReplaceIntoWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .replaceInto(VerifyChildEntity::getChildName, VerifyChildEntity::isActive)
                .values("王五", true);
        assertEquals("REPLACE INTO t_verify_child (child_name_col, active_flag) VALUES (?, ?)", sq.toSql());
    }

    // ---------- 6. Join 的 Lambda 重载（on / and / andIfAbsent / onIfAbsent + TypeFunction） ----------

    @Test
    public void testJoinOnWithLambda() {
        SQL<?> sq = SQL.table("tb_user", "u")
                .select("u.name")
                .innerJoin("tb_role", "r", on -> on.on(VerifyChildEntity::getChildName, VerifyChildEntity::isActive))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON child_name_col = active_flag WHERE u.is_active = ?", sq.toSql());
    }

    @Test
    public void testJoinAndWithLambdaValue() {
        SQL<?> sq = SQL.table("tb_user", "u")
                .select("u.name")
                .innerJoin("tb_role", "r", on -> on.on(VerifyChildEntity::getChildName, "=", "x")
                        .and(VerifyChildEntity::isActive, "=", 1))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON child_name_col = ? AND active_flag = ? WHERE u.is_active = ?", sq.toSql());
    }

    @Test
    public void testJoinAndIfAbsentWithLambdaSkip() {
        SQL<?> sq = SQL.table("tb_user", "u")
                .select("u.name")
                .innerJoin("tb_role", "r", on -> on.on(VerifyChildEntity::getChildName, VerifyChildEntity::isActive)
                        .andIfAbsent(VerifyChildEntity::isActive, "=", null))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON child_name_col = active_flag WHERE u.is_active = ?", sq.toSql());
    }

    // ---------- 7. P1：onDuplicateKeyUpdate/onDuplicateKeyUpdateValues + ConditionGroup.and 的 Lambda ----------

    @Test
    public void testOnDuplicateKeyUpdateWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .insertInto(VerifyChildEntity::getChildName)
                .values("x")
                .onDuplicateKeyUpdate(VerifyChildEntity::getChildName, "y");
        assertEquals("INSERT INTO t_verify_child (child_name_col) VALUES (?) ON DUPLICATE KEY UPDATE child_name_col = ?", sq.toSql());
    }

    @Test
    public void testOnDuplicateKeyUpdateValuesWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .insertInto(VerifyChildEntity::getChildName)
                .values("x")
                .onDuplicateKeyUpdateValues(VerifyChildEntity::getChildName);
        assertEquals("INSERT INTO t_verify_child (child_name_col) VALUES (?) ON DUPLICATE KEY UPDATE child_name_col = VALUES(child_name_col)", sq.toSql());
    }

    @Test
    public void testConditionAndCustomOperatorWithLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .select()
                .where(w -> w.and(VerifyChildEntity::getChildName, "=", "x"));
        assertEquals("SELECT * FROM t_verify_child WHERE child_name_col = ?", sq.toSql());
    }

    // ---------- 8. 更多函数覆盖 ----------

    @Test
    public void testCountDistinct() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.countDistinct("status").as("c"));
        assertEquals("SELECT COUNT(DISTINCT status) AS c FROM t", sq.toSql());
    }

    @Test
    public void testGroupConcatWithSeparator() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.groupConcat("name", ",").as("n"));
        assertEquals("SELECT GROUP_CONCAT(name, SEPARATOR ,) AS n FROM t", sq.toSql());
    }

    @Test
    public void testConcatWs() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.concat_ws("-", "a", "b").as("x"));
        assertEquals("SELECT CONCAT_WS(?, a, b) AS x FROM t", sq.toSql());
        assertEquals("-", sq.getParameters().get(0));
    }

    @Test
    public void testCoalesce() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.coalesce("a", "b").as("c"));
        assertEquals("SELECT COALESCE(a, b) AS c FROM t", sq.toSql());
    }

    @Test
    public void testIfNull() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.ifNull("remark", "no remark").as("r"));
        assertEquals("SELECT IFNULL(remark, ?) AS r FROM t", sq.toSql());
        assertEquals("no remark", sq.getParameters().get(0));
    }

    @Test
    public void testAbsFuncExpr() {
        SQL<?> sq = SQL.table("t").select(FuncBuilder.abs(FuncBuilder.col("x")).as("a"));
        assertEquals("SELECT ABS(x) AS a FROM t", sq.toSql());
    }

    // ---------- 9. JOIN 实体类 + Lambda ON ----------

    @Test
    public void testInnerJoinWithClassAndLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .select("c.child_name_col")
                .innerJoin(VerifyChildEntity.class, "c", on -> on.on(VerifyChildEntity::getChildName, VerifyChildEntity::getChildName)
                        .and(VerifyChildEntity::isActive, "=", true));
        assertEquals("SELECT c.child_name_col FROM t_verify_child INNER JOIN t_verify_child c ON child_name_col = child_name_col AND active_flag = ?", sq.toSql());
        assertEquals(1, sq.getParameters().size());
        assertEquals(true, sq.getParameters().get(0));
    }

    @Test
    public void testLeftJoinWithClassParentField() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class)
                .select()
                .leftJoin(VerifyChildEntity.class, "p", on -> on.on(VerifyChildEntity::getParentCode, VerifyChildEntity::getChildName));
        assertEquals("SELECT * FROM t_verify_child LEFT JOIN t_verify_child p ON parent_code_col = child_name_col", sq.toSql());
    }

    // ---------- 10. WHERE 条件 IfAbsent（值为空则跳过） ----------

    @Test
    public void testLikeIfAbsentSkipWhenNull() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.eq("status", 1).likeIfAbsent("name", null));
        assertEquals("SELECT id FROM tb_user WHERE status = ?", sq.toSql());
    }

    @Test
    public void testLikeIfAbsentIncludeWhenPresent() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.eq("status", 1).likeIfAbsent("name", "张"));
        assertEquals("SELECT id FROM tb_user WHERE status = ? AND name LIKE ?", sq.toSql());
        assertEquals("%张%", sq.getParameters().get(1));
    }

    @Test
    public void testEqIfAbsentLambda() {
        SQL<VerifyChildEntity> sq = SQL.table(VerifyChildEntity.class).select()
                .where(w -> w.eqIfAbsent(VerifyChildEntity::getChildName, null)
                        .eqIfAbsent(VerifyChildEntity::getChildName, "x"));
        assertEquals("SELECT * FROM t_verify_child WHERE child_name_col = ?", sq.toSql());
        assertEquals("x", sq.getParameters().get(0));
    }

    @Test
    public void testInIfAbsentSkipWhenEmpty() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.eq("status", 1).inIfAbsent("role_id", Collections.emptyList()));
        assertEquals("SELECT id FROM tb_user WHERE status = ?", sq.toSql());
    }

    @Test
    public void testInIfAbsentIncludeWhenNotEmpty() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.inIfAbsent("role_id", Arrays.asList(1, 2, 3)));
        assertEquals("SELECT id FROM tb_user WHERE role_id IN (?, ?, ?)", sq.toSql());
    }

    @Test
    public void testBetweenAndIfAbsentSkipWhenLeftNull() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.eq("status", 1).betweenAndIfAbsent("age", null, 60));
        assertEquals("SELECT id FROM tb_user WHERE status = ?", sq.toSql());
    }

    @Test
    public void testAndIfAbsentCustomOperator() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.andIfAbsent("age", ">", 18).andIfAbsent("age", ">", null));
        assertEquals("SELECT id FROM tb_user WHERE age > ?", sq.toSql());
    }

    @Test
    public void testAndBooleanBlockFlag() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.eq("status", 1).and(false, g -> g.like("name", "张")).or(true, g -> g.gt("age", 18)));
        assertEquals("SELECT id FROM tb_user WHERE status = ? OR (age > ?)", sq.toSql());
    }
}
