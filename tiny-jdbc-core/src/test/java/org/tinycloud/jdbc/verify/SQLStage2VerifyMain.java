package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.FuncBuilder;
import org.tinycloud.jdbc.sql.SQL;

import static org.junit.Assert.assertEquals;

/**
 * SQL 构建器能力测试（阶段2）：UNION / UNION ALL、INSERT IGNORE / REPLACE + ON DUPLICATE KEY UPDATE、
 * 连接更新、子查询赋值、CASE WHEN。
 */
public class SQLStage2VerifyMain {

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

    // ---------- 补充覆盖 ----------

    @Test
    public void testUnionWithOrderBy() {
        SQL<?> u = SQL.table("a").select("id").where(c -> c.eq("x", 1))
                .unionAll(SQL.table("b").select("id").where(c -> c.eq("y", 2)))
                .orderBy("id").desc();
        // orderBy 作用于整个 UNION 结果，渲染在所有片段之后
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
}
