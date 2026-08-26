package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.SQL;

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
 * 校验 SQL.md 文档中的关键示例，确保 toSql()/getParameters() 输出与文档注释一致。
 */
public class SQLDocumentVerifyMain {

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

    // ---------- 补充覆盖 ----------

    @Test
    public void testFieldReferenceWhere() {
        SQL<?> sq = SQL.table("t_score").select("id")
                .where(w -> w.eq("student_id", new FieldReference("student_code"))
                        .gt("avg_score", new FieldReference("max_score")));
        assertEquals("SELECT id FROM t_score WHERE student_id = student_code AND avg_score > max_score", sq.toSql());
    }

    @Test
    public void testOrGroup() {
        SQL<?> sq = SQL.table("t_user").select()
                .where(w -> w.group(g -> g.eq("status", "ACTIVE").or().eq("status", "PENDING"))
                        .and(g -> g.ge("age", 18).lt("age", 60)));
        assertEquals("SELECT * FROM t_user WHERE (status = ? OR status = ?) AND (age >= ? AND age < ?)", sq.toSql());
    }

    @Test
    public void testLimitOffset() {
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
}
