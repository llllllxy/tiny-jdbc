package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.FuncBuilder;
import org.tinycloud.jdbc.sql.SQL;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.tinycloud.jdbc.sql.FuncBuilder.avg;
import static org.tinycloud.jdbc.sql.FuncBuilder.coalesce;
import static org.tinycloud.jdbc.sql.FuncBuilder.col;
import static org.tinycloud.jdbc.sql.FuncBuilder.concat;
import static org.tinycloud.jdbc.sql.FuncBuilder.count;
import static org.tinycloud.jdbc.sql.FuncBuilder.dateFormat;
import static org.tinycloud.jdbc.sql.FuncBuilder.ifNull;
import static org.tinycloud.jdbc.sql.FuncBuilder.lit;
import static org.tinycloud.jdbc.sql.FuncBuilder.sum;
import static org.tinycloud.jdbc.sql.FuncBuilder.year;

/**
 * SQL 构建器能力验证（合并版）。
 * 由 SQLAdvancedVerifyMain、SQLStage2VerifyMain、SQLDocumentVerifyMain、SQLFeatureVerifyMain 合并而来。
 * 注：原 SQLDocumentVerifyMain 中的 testOrGroup / testLimitOffset 与 SQLAdvancedVerifyMain 重名，已重命名为
 * testOrGroupDocument / testLimitOffsetDocument 以保留全部测试方法。
 */
public class SQLBuilderVerifyMain {

    // ---------- SQLAdvancedVerifyMain ----------

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

    // ---------- SQLStage2VerifyMain ----------

    @Test
    public void testUnionAll() {
        SQL<?> u1 = SQL.table("a").select("id").where(c -> c.eq("x", 1))
                .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)));
        assertEquals("SELECT id FROM a WHERE x = ? UNION ALL SELECT id FROM b WHERE y = ?", u1.toSql());
        assertEquals(2, u1.getParameters().size());
        assertEquals(1, u1.getParameters().get(0));
        assertEquals(2, u1.getParameters().get(1));
    }

    @Test
    public void testUnionDistinct() {
        SQL<?> u2 = SQL.table("a").select("id").where(c -> c.lt("age", 18))
                .union(SQL.table("b").select("id").where(c -> c.gt("age", 60)));
        assertEquals("SELECT id FROM a WHERE age < ? UNION SELECT id FROM b WHERE age > ?", u2.toSql());
        assertEquals(2, u2.getParameters().size());
        assertEquals(18, u2.getParameters().get(0));
        assertEquals(60, u2.getParameters().get(1));
    }

    @Test
    public void testUnionAllThreeSegments() {
        SQL<?> u3 = SQL.table("a").select("id").where(c -> c.eq("x", 1))
                .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)))
                .unionAll(SQL.table("c").select("id").where(c -> c.eq("z", 3)));
        assertEquals("SELECT id FROM a WHERE x = ? UNION ALL SELECT id FROM b WHERE y = ? UNION ALL SELECT id FROM c WHERE z = ?", u3.toSql());
        assertEquals(3, u3.getParameters().size());
        assertEquals(1, u3.getParameters().get(0));
        assertEquals(2, u3.getParameters().get(1));
        assertEquals(3, u3.getParameters().get(2));
    }

    @Test
    public void testInsertIgnoreWithOnDuplicateKeyUpdate() {
        SQL<?> ins = SQL.table("tb_user").insertIgnoreInto("id", "name").values(5, "x")
                .onDuplicateKeyUpdate("name", "y");
        assertEquals("INSERT IGNORE INTO tb_user (id, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = ?", ins.toSql());
        assertEquals(3, ins.getParameters().size());
        assertEquals(5, ins.getParameters().get(0));
        assertEquals("x", ins.getParameters().get(1));
        assertEquals("y", ins.getParameters().get(2));
    }

    @Test
    public void testReplaceWithOnDuplicateKeyUpdateValues() {
        SQL<?> rep = SQL.table("tb_user").replaceInto("id", "name").values(5, "x")
                .onDuplicateKeyUpdateValues("name");
        assertEquals("REPLACE INTO tb_user (id, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)", rep.toSql());
        assertEquals(2, rep.getParameters().size());
    }

    @Test
    public void testUpdateJoinWithColumnReference() {
        SQL<?> upd = SQL.table("tb_user")
                .update()
                .set("name", new FieldReference("r.role_name"))
                .innerJoin("tb_role", "r").on("tb_user.role_id", "r.id")
                .where(c -> c.eq("tb_user.role_id", 3));
        assertEquals("UPDATE tb_user INNER JOIN tb_role r ON tb_user.role_id = r.id SET name = r.role_name WHERE tb_user.role_id = ?", upd.toSql());
        assertEquals(1, upd.getParameters().size());
        assertEquals(3, upd.getParameters().get(0));
    }

    @Test
    public void testUpdateSubQueryAssignment() {
        SQL<?> upd2 = SQL.table("tb_user")
                .update()
                .set("email", SQL.table("tb_user_info").select("email").where(c -> c.eq("user_id", 9)))
                .where(c -> c.eq("id", 9));
        assertEquals("UPDATE tb_user SET email = (SELECT email FROM tb_user_info WHERE user_id = ?) WHERE id = ?", upd2.toSql());
        assertEquals(2, upd2.getParameters().size());
        assertEquals(9, upd2.getParameters().get(0));
        assertEquals(9, upd2.getParameters().get(1));
    }

    @Test
    public void testCaseWhen() {
        SQL<?> cs = SQL.table("tb_order").select(
                FuncBuilder.caseWhen()
                        .when("status = 1", FuncBuilder.lit("active"))
                        .when("status = 2", FuncBuilder.lit("pending"))
                        .otherwise(FuncBuilder.lit("unknown"))
                        .build().as("status_desc"));
        assertEquals("SELECT CASE WHEN status = 1 THEN ? WHEN status = 2 THEN ? ELSE ? END AS status_desc FROM tb_order", cs.toSql());
        assertEquals(3, cs.getParameters().size());
        assertEquals("active", cs.getParameters().get(0));
        assertEquals("pending", cs.getParameters().get(1));
        assertEquals("unknown", cs.getParameters().get(2));
    }

    @Test
    public void testUnionWithOrderBy() {
        SQL<?> u = SQL.table("a").select("id").where(c -> c.eq("x", 1))
                .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)))
                .orderBy("id").desc();
        assertEquals("SELECT id FROM a WHERE x = ? UNION ALL SELECT id FROM b WHERE y = ? ORDER BY id DESC", u.toSql());
    }

    @Test
    public void testInsertMultiRowWithOnDuplicateKeyUpdate() {
        SQL<?> ins = SQL.table("tb_user").insert("id", "name").values(1, "a").values(2, "b")
                .onDuplicateKeyUpdate("name", "x");
        assertEquals("INSERT INTO tb_user (id, name) VALUES (?, ?), (?, ?) ON DUPLICATE KEY UPDATE name = ?", ins.toSql());
        assertEquals(5, ins.getParameters().size());
    }

    @Test
    public void testReplaceMultiRow() {
        SQL<?> rep = SQL.table("tb_user").replaceInto("id", "name").values(1, "a").values(2, "b");
        assertEquals("REPLACE INTO tb_user (id, name) VALUES (?, ?), (?, ?)", rep.toSql());
    }

    @Test
    public void testUpdateSetTypeFunction() {
        SQL<VerifyChildEntity> upd = SQL.table(VerifyChildEntity.class)
                .update()
                .set(VerifyChildEntity::getChildName, "x")
                .where(w -> w.eq(VerifyChildEntity::getChildName, "y"));
        assertEquals("UPDATE t_verify_child SET child_name_col = ? WHERE child_name_col = ?", upd.toSql());
        assertEquals(2, upd.getParameters().size());
        assertEquals("x", upd.getParameters().get(0));
        assertEquals("y", upd.getParameters().get(1));
    }

    @Test
    public void testUpdateSetSubQueryWithAlias() {
        SQL<?> upd = SQL.table("tb_user", "u").update()
                .set("u.email", SQL.table("tb_user_info").select("email").where(c -> c.eq("user_id", 9)))
                .where(w -> w.eq("u.id", 9));
        assertEquals("UPDATE tb_user u SET u.email = (SELECT email FROM tb_user_info WHERE user_id = ?) WHERE u.id = ?", upd.toSql());
    }

    // ---------- SQLDocumentVerifyMain ----------

    @Test
    public void testJoinCallback() {
        SQL<?> joinCb = SQL.table("tb_user").select("u.name", "r.role_name")
                .innerJoin("tb_role", "r", on -> on.on("u.role_id", "r.id").and("r.status", "=", 1))
                .where(w -> w.eq("u.is_active", 1));
        assertEquals("SELECT u.name, r.role_name FROM tb_user INNER JOIN tb_role r ON u.role_id = r.id AND r.status = ? WHERE u.is_active = ?", joinCb.toSql());
        assertEquals(2, joinCb.getParameters().size());
    }

    @Test
    public void testExists() {
        SQL<?> exists = SQL.table("tb_user").select()
                .where(w -> w.eq("is_active", 1)
                        .exists(SQL.table("tb_role").select("id").where(c2 -> c2.eq("status", "ENABLED"))));
        assertEquals("SELECT * FROM tb_user WHERE is_active = ? AND EXISTS (SELECT id FROM tb_role WHERE status = ?)", exists.toSql());
        assertEquals(2, exists.getParameters().size());
        assertEquals(1, exists.getParameters().get(0));
        assertEquals("ENABLED", exists.getParameters().get(1));
    }

    @Test
    public void testNestedSubQuery() {
        SQL<?> nested = SQL.table("t").select("a.*")
                .from(SQL.table("tb_user").select("b.*")
                        .from(SQL.table("tb_order").select("c.*").where(c -> c.eq("status", "PAID")), "c")
                        .where(c -> c.gt("age", 18)), "b")
                .where(c -> c.eq("platform", "APP"));
        assertEquals("SELECT a.* FROM (SELECT b.* FROM (SELECT c.* FROM tb_order WHERE status = ?) c WHERE age > ?) b WHERE platform = ?", nested.toSql());
        assertEquals(3, nested.getParameters().size());
    }

    @Test
    public void testLockInShareMode() {
        SQL<?> share = SQL.table("tb_user").select("id").lockInShareMode();
        assertEquals("SELECT id FROM tb_user LOCK IN SHARE MODE", share.toSql());
    }

    @Test
    public void testConcatWithLiteral() {
        SQL<?> fn = SQL.table("t_user")
                .select(concat(col("real_name"), lit(", "), col("name")).as("display"));
        assertEquals("SELECT CONCAT(real_name, ?, name) AS display FROM t_user", fn.toSql());
        assertEquals(", ", fn.getParameters().get(0));
    }

    @Test
    public void testDateFunctions() {
        SQL<?> fn2 = SQL.table("t_user")
                .select(dateFormat("create_time", "%Y-%m-%d").as("day"),
                        year("create_time").as("current_year"));
        assertEquals("SELECT DATE_FORMAT(create_time, ?) AS day, YEAR(create_time) AS current_year FROM t_user", fn2.toSql());
        assertEquals("%Y-%m-%d", fn2.getParameters().get(0));
    }

    @Test
    public void testAggregateWithOrderBy() {
        SQL<?> report = SQL.table("user")
                .select(col("name"), count("*").as("total"), avg("age").as("avg_age"))
                .where(w -> w.eq("status", "active"))
                .groupBy("name")
                .orderBy("total").desc();
        assertEquals("SELECT name, COUNT(*) AS total, AVG(age) AS avg_age FROM user WHERE status = ? GROUP BY name ORDER BY total DESC", report.toSql());
        assertEquals("active", report.getParameters().get(0));
    }

    @Test
    public void testHaving() {
        SQL<?> having = SQL.table("t_order")
                .select(col("user_id"), count("*").as("total"), sum("amount").as("total_amount"))
                .where(w -> w.eq("status", "PAID"))
                .groupBy("user_id")
                .having(w -> w.gt("total_amount", 1000));
        assertEquals("SELECT user_id, COUNT(*) AS total, SUM(amount) AS total_amount FROM t_order WHERE status = ? GROUP BY user_id HAVING total_amount > ?", having.toSql());
        assertEquals(2, having.getParameters().size());
    }

    @Test
    public void testFieldReferenceWhere() {
        SQL<?> sq = SQL.table("t_score").select("id")
                .where(w -> w.eq("student_id", new FieldReference("student_code"))
                        .gt("avg_score", new FieldReference("max_score")));
        assertEquals("SELECT id FROM t_score WHERE student_id = student_code AND avg_score > max_score", sq.toSql());
    }

    @Test
    public void testOrGroupDocument() {
        SQL<?> sq = SQL.table("t_user").select()
                .where(w -> w.group(g -> g.eq("status", "ACTIVE").or().eq("status", "PENDING"))
                        .and(g -> g.ge("age", 18).lt("age", 60)));
        assertEquals("SELECT * FROM t_user WHERE (status = ? OR status = ?) AND (age >= ? AND age < ?)", sq.toSql());
    }

    @Test
    public void testLimitOffsetDocument() {
        SQL<?> sq = SQL.table("t_user").select("id").orderBy("id").desc().limit(10).offset(20);
        assertEquals("SELECT id FROM t_user ORDER BY id DESC LIMIT 10 OFFSET 20", sq.toSql());
    }

    @Test
    public void testIfNullCoalesce() {
        SQL<?> sq = SQL.table("t_user")
                .select(ifNull("remark", "no remark").as("r"), coalesce("a", "b").as("c"));
        assertEquals("SELECT IFNULL(remark, ?) AS r, COALESCE(a, b) AS c FROM t_user", sq.toSql());
        assertEquals("no remark", sq.getParameters().get(0));
    }

    @Test
    public void testSelectDistinctColumns() {
        SQL<?> sq = SQL.table("t_order").selectDistinct("user_id", "status");
        assertEquals("SELECT DISTINCT user_id, status FROM t_order", sq.toSql());
    }

    // ---------- SQLFeatureVerifyMain ----------

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
