package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.FuncBuilder;
import org.tinycloud.jdbc.sql.SQL;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * SQL 构建器能力测试（阶段1）：聚合/函数/子查询/JOIN/distinct/行锁/列引用。
 */
public class SQLAdvancedVerifyMain {

    @Test
    public void testAggregateWithAlias() {
        SQL<?> agg = SQL.table("tb_user").select(
                FuncBuilder.count("id").as("total"),
                FuncBuilder.max("age").as("maxAge"));
        assertEquals("SELECT COUNT(id) AS total, MAX(age) AS maxAge FROM tb_user", agg.toSql());
        assertEquals(0, agg.getParameters().size());
    }

    @Test
    public void testFunctionWithLiteralParam() {
        SQL<?> fn = SQL.table("tb_order").select(
                FuncBuilder.concat(
                        FuncBuilder.col("realName"),
                        FuncBuilder.lit(", "),
                        FuncBuilder.col("name")).as("display"));
        assertEquals("SELECT CONCAT(realName, ?, name) AS display FROM tb_order", fn.toSql());
        assertEquals(1, fn.getParameters().size());
        assertEquals(", ", fn.getParameters().get(0));
    }

    @Test
    public void testJoin() {
        SQL<?> join = SQL.table("tb_user")
                .select("name", "r.role_name")
                .leftJoin("tb_role", "r")
                .on("tb_user.role_id", "r.id")
                .where(c -> c.eq("is_active", 1));
        assertEquals("SELECT name, r.role_name FROM tb_user LEFT JOIN tb_role r ON tb_user.role_id = r.id WHERE is_active = ?", join.toSql());
        assertEquals(1, join.getParameters().size());
        assertEquals(1, join.getParameters().get(0));
    }

    @Test
    public void testInSubQuery() {
        SQL<?> in = SQL.table("tb_user").select()
                .where(c -> c.in("id",
                        SQL.table("tb_order").select("user_id").where(c2 -> c2.eq("status", "PAID"))));
        assertEquals("SELECT * FROM tb_user WHERE id IN (SELECT user_id FROM tb_order WHERE status = ?)", in.toSql());
        assertEquals(1, in.getParameters().size());
        assertEquals("PAID", in.getParameters().get(0));
    }

    @Test
    public void testSubQueryFrom() {
        SQL<?> subFrom = SQL.table("d")
                .select("a.*")
                .from(SQL.table("tb_user").select("u.id").where(c -> c.gt("age", 18)), "a");
        assertEquals("SELECT a.* FROM (SELECT u.id FROM tb_user WHERE age > ?) a", subFrom.toSql());
        assertEquals(1, subFrom.getParameters().size());
        assertEquals(18, subFrom.getParameters().get(0));
    }

    @Test
    public void testDistinct() {
        SQL<?> dist = SQL.table("tb_user").selectDistinct("age");
        assertEquals("SELECT DISTINCT age FROM tb_user", dist.toSql());
    }

    @Test
    public void testForUpdateLock() {
        SQL<?> lock = SQL.table("tb_user").select("id").where(c -> c.eq("id", 99)).forUpdate();
        assertEquals("SELECT id FROM tb_user WHERE id = ? FOR UPDATE", lock.toSql());
    }

    @Test
    public void testColumnReferenceUpdate() {
        SQL<?> ref = SQL.table("tb_score").update()
                .set("max_score", new FieldReference("score"))
                .where(c -> c.eq("student_id", 7));
        assertEquals("UPDATE tb_score SET max_score = score WHERE student_id = ?", ref.toSql());
        assertEquals(1, ref.getParameters().size());
        assertEquals(7, ref.getParameters().get(0));
    }

    // ---------- 补充覆盖 ----------

    @Test
    public void testRightJoin() {
        SQL<?> sq = SQL.table("tb_user").select("u.name", "r.role_name")
                .rightJoin("tb_role", "r").on("tb_user.role_id", "r.id")
                .where(c -> c.eq("tb_user.is_active", 1));
        assertEquals("SELECT u.name, r.role_name FROM tb_user RIGHT JOIN tb_role r ON tb_user.role_id = r.id WHERE tb_user.is_active = ?", sq.toSql());
        assertEquals(1, sq.getParameters().size());
        assertEquals(1, sq.getParameters().get(0));
    }

    @Test
    public void testCrossJoin() {
        SQL<?> sq = SQL.table("a").select("*").crossJoin("b", "bb");
        assertEquals("SELECT * FROM a CROSS JOIN b bb", sq.toSql());
    }

    @Test
    public void testJoinCallbackOn() {
        SQL<?> sq = SQL.table("tb_user", "u").select("u.name")
                .innerJoin("tb_role", "r", on -> on.on("u.role_id", "r.id").and("r.status", "=", 1))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name FROM tb_user u INNER JOIN tb_role r ON u.role_id = r.id AND r.status = ? WHERE u.is_active = ?", sq.toSql());
    }

    @Test
    public void testLimitOffset() {
        SQL<?> sq = SQL.table("tb_user").select("id").orderBy("id").desc().limit(10).offset(20);
        assertEquals("SELECT id FROM tb_user ORDER BY id DESC LIMIT 10 OFFSET 20", sq.toSql());
    }

    @Test
    public void testInCollection() {
        SQL<?> sq = SQL.table("tb_user").select("id").where(c -> c.in("status", Arrays.asList(1, 2, 3)));
        assertEquals("SELECT id FROM tb_user WHERE status IN (?, ?, ?)", sq.toSql());
        assertEquals(3, sq.getParameters().size());
    }

    @Test
    public void testBetweenAnd() {
        SQL<?> sq = SQL.table("tb_user").select("id").where(c -> c.betweenAnd("age", 18, 60));
        assertEquals("SELECT id FROM tb_user WHERE age BETWEEN ? AND ?", sq.toSql());
        assertEquals(18, sq.getParameters().get(0));
        assertEquals(60, sq.getParameters().get(1));
    }

    @Test
    public void testIsNullIsNotNull() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(c -> c.isNull("deleted_at").isNotNull("mobile"));
        assertEquals("SELECT id FROM tb_user WHERE deleted_at IS NULL AND mobile IS NOT NULL", sq.toSql());
    }

    @Test
    public void testOrGroup() {
        SQL<?> sq = SQL.table("tb_user").select("id")
                .where(w -> w.group(g -> g.eq("status", "ACTIVE").or().eq("status", "PENDING"))
                        .and(g -> g.ge("age", 18).lt("age", 60)));
        assertEquals("SELECT id FROM tb_user WHERE (status = ? OR status = ?) AND (age >= ? AND age < ?)", sq.toSql());
    }

    @Test
    public void testNotInSubQuery() {
        SQL<?> sq = SQL.table("tb_user").delete()
                .where(c -> c.notIn("id", SQL.table("tb_order").select("user_id").where(c2 -> c2.eq("status", "PAID"))));
        assertEquals("DELETE FROM tb_user WHERE id NOT IN (SELECT user_id FROM tb_order WHERE status = ?)", sq.toSql());
    }

    @Test
    public void testCountStar() {
        SQL<?> sq = SQL.table("tb_user").select(FuncBuilder.count().as("cnt"));
        assertEquals("SELECT COUNT(*) AS cnt FROM tb_user", sq.toSql());
    }

    @Test
    public void testGroupConcat() {
        SQL<?> sq = SQL.table("tb_user").select(FuncBuilder.groupConcat("name").as("names"));
        assertEquals("SELECT GROUP_CONCAT(name) AS names FROM tb_user", sq.toSql());
    }

    @Test
    public void testAggregateMinSum() {
        SQL<?> sq = SQL.table("tb_user").select(FuncBuilder.min("age").as("minAge"), FuncBuilder.sum("score").as("sumScore"));
        assertEquals("SELECT MIN(age) AS minAge, SUM(score) AS sumScore FROM tb_user", sq.toSql());
    }

    @Test
    public void testStringFunctions() {
        SQL<?> sq = SQL.table("t_user").select(
                FuncBuilder.substring("name", 1, 3).as("part"),
                FuncBuilder.left("code", 2).as("l"),
                FuncBuilder.right("code", 2).as("r"),
                FuncBuilder.upper("name").as("up"),
                FuncBuilder.length("name").as("len"));
        assertEquals("SELECT SUBSTRING(name, ?, ?) AS part, LEFT(code, ?) AS l, RIGHT(code, ?) AS r, UPPER(name) AS up, LENGTH(name) AS len FROM t_user", sq.toSql());
        assertEquals(4, sq.getParameters().size());
    }
}
