package org.tinycloud.jdbc.verify;
import org.junit.Test;

import org.tinycloud.jdbc.criteria.query.QueryCriteria;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.RawSql;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

/**
 * 使用 main 方法快速验证「标识符安全边界」：
 * <ul>
 *   <li>合法表名 / 列引用 / 别名 / 裸列名不抛异常；</li>
 *   <li>非法标识符（含分号、引号、空白、注释、控制字符、前导数字、括号等）被拒绝；</li>
 *   <li>{@code last()} 默认严格校验尾部片段，{@code last(RawSql)} 显式授权绕过；</li>
 *   <li>边界：{@code null} / 空串 / 纯空白 / 保留字（关键字）均按预期处理。</li>
 * </ul>
 */
public class SqlIdentifierVerifyMain {

    @Test public void testValidIdentifiers() {
        // 列引用：裸列名、限定引用、通配
        SqlIdentifierUtils.checkColumnRef("name");
        SqlIdentifierUtils.checkColumnRef("total_amount");
        SqlIdentifierUtils.checkColumnRef("r.role_name");
        SqlIdentifierUtils.checkColumnRef("tb_user.role_id");
        SqlIdentifierUtils.checkColumnRef("a.*");
        SqlIdentifierUtils.checkColumnRef("c.child_name_col");
        SqlIdentifierUtils.checkColumnRef("*");
        SqlIdentifierUtils.checkColumnRef("id");

        // 表名：裸表名 + 限定（库.表）
        SqlIdentifierUtils.checkTableName("tb_user");
        SqlIdentifierUtils.checkTableName("t_verify_child");
        SqlIdentifierUtils.checkTableName("db.tb_user");

        // 别名
        SqlIdentifierUtils.checkAlias("u");
        SqlIdentifierUtils.checkAlias("t_user");

        // 裸列名
        SqlIdentifierUtils.checkColumnName("id");
        SqlIdentifierUtils.checkColumnName("user_id");
        SqlIdentifierUtils.checkColumnName("status");
    }

    @Test public void testIllegalIdentifiersRejected() {
        // 列引用：分号、引号、空白、注释、前导数字
        assertThrowsColumnRef("name; drop table users");
        assertThrowsColumnRef("a b");
        assertThrowsColumnRef("a--b");
        assertThrowsColumnRef("1abc");
        assertThrowsColumnRef("x') or '1'='1");
        assertThrowsColumnRef("name`");

        // 表名
        assertThrowsTableName("tb_user; drop");
        assertThrowsTableName("1abc");
        assertThrowsTableName("a b");

        // 别名
        assertThrowsAlias("u; drop");
        assertThrowsAlias("1abc");

        // 裸列名
        assertThrowsColumnName("id;");
        assertThrowsColumnName("a b");
    }

    // ---------- 列引用边界 ----------

    @Test public void testColumnRefBoundaries() {
        // 合法：多种 ATOM 形态
        assertValidColumnRef("a");
        assertValidColumnRef("_a");
        assertValidColumnRef("a1");
        assertValidColumnRef("_a1");
        assertValidColumnRef("a_b_c");
        assertValidColumnRef("A");
        assertValidColumnRef("abcDef");
        assertValidColumnRef("T1");
        // 关键字 / 保留字是合法标识符（不做关键字黑名单）
        assertValidColumnRef("user");
        assertValidColumnRef("order");
        assertValidColumnRef("select");
        assertValidColumnRef("group");
        // 限定 + 通配
        assertValidColumnRef("a.b");
        assertValidColumnRef("r.role_name");
        assertValidColumnRef("tb_user.role_id");
        assertValidColumnRef("schema.table");
        assertValidColumnRef("a.b.c");
        assertValidColumnRef("db.tbl.col");
        assertValidColumnRef("a.*");
        assertValidColumnRef("b.*");
        assertValidColumnRef("c.*");
        assertValidColumnRef("a.b.*");
        assertValidColumnRef("x.y.*");
        assertValidColumnRef("db.tbl.col.*");
        assertValidColumnRef("*");

        // 非法：前导数字
        assertThrowsColumnRef("1abc");
        assertThrowsColumnRef("1.a");
        // 非法：连字符 / 空格
        assertThrowsColumnRef("a-b");
        assertThrowsColumnRef("a b");
        assertThrowsColumnRef(" a");
        assertThrowsColumnRef("a ");
        assertThrowsColumnRef("a b.c");
        // 非法：点号位置错误
        assertThrowsColumnRef(".a");
        assertThrowsColumnRef("a.");
        assertThrowsColumnRef("a..b");
        assertThrowsColumnRef("a.b.");
        assertThrowsColumnRef(".a.b");
        // 非法：多余 / 组合通配
        assertThrowsColumnRef("a.*.*");
        assertThrowsColumnRef("*.*");
        assertThrowsColumnRef("*.a");
        assertThrowsColumnRef("a..*");
        assertThrowsColumnRef("*abc");
        // 非法：引号 / 反引号
        assertThrowsColumnRef("'a'");
        assertThrowsColumnRef("\"a\"");
        assertThrowsColumnRef("`a`");
        assertThrowsColumnRef("a'b");
        assertThrowsColumnRef("a\"b");
        assertThrowsColumnRef("a`b");
        // 非法：注释
        assertThrowsColumnRef("a--b");
        assertThrowsColumnRef("a#b");
        assertThrowsColumnRef("a/*b");
        assertThrowsColumnRef("a*/b");
        assertThrowsColumnRef("a/**/b");
        // 非法：括号 / 逗号 / 函数式
        assertThrowsColumnRef("a(b)");
        assertThrowsColumnRef("COUNT(*)");
        assertThrowsColumnRef("a,b");
        // 非法：控制字符
        assertThrowsColumnRef("a\nb");
        assertThrowsColumnRef("a\tb");
        assertThrowsColumnRef("a\rb");
        assertThrowsColumnRef("a\0b");
        // 非法：分号 / 非标识符字符
        assertThrowsColumnRef("a;b");
        assertThrowsColumnRef("a; drop");
        assertThrowsColumnRef("a$b");
        assertThrowsColumnRef("$a");
    }

    // ---------- 表名边界 ----------

    @Test public void testTableNameBoundaries() {
        assertValidTable("t_user");
        assertValidTable("tb_user");
        assertValidTable("t_verify_child");
        assertValidTable("db.t_user");
        assertValidTable("schema.table");
        assertValidTable("a.b.c");
        assertValidTable("_x");
        assertValidTable("t1");
        // 保留字
        assertValidTable("user");
        assertValidTable("order");

        assertThrowsTableName("1t");
        assertThrowsTableName("t-user");
        assertThrowsTableName("t user");
        assertThrowsTableName("t;drop");
        assertThrowsTableName("`t`");
        assertThrowsTableName("t.*");
        assertThrowsTableName("t.u.*");
        assertThrowsTableName(".t");
        assertThrowsTableName("t.");
        assertThrowsTableName("t..u");
        assertThrowsTableName("t\n");
        assertThrowsTableName("t#");
        assertThrowsTableName("t--");
        assertThrowsTableName("t/*");
        assertThrowsTableName("'t'");
    }

    // ---------- 别名边界 ----------

    @Test public void testAliasBoundaries() {
        assertValidAlias("a");
        assertValidAlias("u");
        assertValidAlias("t_user");
        assertValidAlias("_x");
        assertValidAlias("x1");
        assertValidAlias("tb");
        assertValidAlias("T");

        assertThrowsAlias("1a");
        assertThrowsAlias("a-b");
        assertThrowsAlias("a b");
        assertThrowsAlias("a;drop");
        assertThrowsAlias("a.b");
        assertThrowsAlias("a.*");
        assertThrowsAlias("*");
        assertThrowsAlias(".a");
        assertThrowsAlias("a.");
        assertThrowsAlias("'a'");
        assertThrowsAlias("`a`");
        assertThrowsAlias("a--");
        assertThrowsAlias("a#");
        assertThrowsAlias("a\n");
        assertThrowsAlias("a@");
    }

    // ---------- 裸列名边界 ----------

    @Test public void testColumnNameBoundaries() {
        assertValidColumnName("a");
        assertValidColumnName("id");
        assertValidColumnName("user_id");
        assertValidColumnName("status");
        assertValidColumnName("total_amount");
        assertValidColumnName("_x");
        assertValidColumnName("x1");

        assertThrowsColumnName("1a");
        assertThrowsColumnName("a-b");
        assertThrowsColumnName("a.b");     // 裸列名不允许限定
        assertThrowsColumnName("a.*");     // 裸列名不允许通配
        assertThrowsColumnName("*");
        assertThrowsColumnName("a b");
        assertThrowsColumnName("a;");
        assertThrowsColumnName("'a'");
        assertThrowsColumnName("`a`");
        assertThrowsColumnName("a--");
    }

    // ---------- last() 尾部片段边界 ----------

    @Test public void testTailSqlBoundaries() {
        // 合法：常见受限尾部子句
        assertValidTail("FOR UPDATE");
        assertValidTail("LIMIT 5");
        assertValidTail("LIMIT 5, 10");
        assertValidTail("GROUP BY a");
        assertValidTail("ORDER BY a DESC");
        assertValidTail("BY x");
        assertValidTail("OFFSET 10");
        assertValidTail("HAVING COUNT(*) > 1");
        assertValidTail("x = 1");

        // 非法：null / 空白
        assertThrowsTail(null);
        assertThrowsTail("");
        assertThrowsTail("   ");
        assertThrowsTail("\t");
        // 非法：分号
        assertThrowsTail("; DROP TABLE users");
        assertThrowsTail("x;");
        // 非法：引号 / 反引号
        assertThrowsTail("x'");
        assertThrowsTail("x\"");
        assertThrowsTail("`x`");
        // 非法：注释
        assertThrowsTail("x--y");
        assertThrowsTail("x#y");
        assertThrowsTail("x/*");
        assertThrowsTail("x*/");
        // 非法：控制字符
        assertThrowsTail("x\nDROP TABLE users");
        assertThrowsTail("x\t");
        assertThrowsTail("x\r");
        assertThrowsTail("x\0");
    }

    // ---------- SQL 构建器接入 ----------

    @Test public void testSqlBuilderWired() {
        // 合法 —— 不抛异常
        String sql = SQL.table("t_verify_child").select("id", "name", "c.child_name_col").toSql();
        assertEquals("SELECT id, name, c.child_name_col FROM t_verify_child", sql);

        // 非法表名被拒绝
        assertThrows(() -> SQL.table("t_user; drop table users").toSql(), "illegal table name should throw");

        // 非法列引用被拒绝
        assertThrows(() -> SQL.table("t_user").select("name; drop").toSql(), "illegal select column should throw");
        assertThrows(() -> SQL.table("t_user").select("a..b").toSql(), "illegal qualified column should throw");

        // 非法 SET 列被拒绝
        assertThrows(() -> SQL.table("t_user").update().set("name; drop", "x").toSql(), "illegal update set column should throw");

        // 非法 GROUP BY / ORDER BY 列被拒绝
        assertThrows(() -> SQL.table("t_user").select().groupBy("name; drop").toSql(), "illegal group by column should throw");
        assertThrows(() -> SQL.table("t_user").select().orderBy("id; drop").toSql(), "illegal order by column should throw");

        // 非法 JOIN 表 / ON 字段被拒绝
        assertThrows(() -> SQL.table("t_user").select().innerJoin("t_role; drop", "r").toSql(), "illegal join table should throw");
        assertThrows(() -> SQL.table("t_user").select()
                .innerJoin("t_role", "r").on("u.role_id; drop", "r.id").toSql(), "illegal on field should throw");
    }

    // ---------- Criteria 接入 ----------

    @Test public void testCriteriaWired() {
        // 合法字段 —— 不抛异常
        String whereSql = new QueryCriteria<VerifyDemoEntity>().eq("status", "PAID").whereSql();
        assertEquals(" WHERE status = ?", whereSql);
        // 限定字段亦合法
        assertEquals(" WHERE tb_user.role_id = ?",
                new QueryCriteria<VerifyDemoEntity>().eq("tb_user.role_id", 3).whereSql());

        // 非法字段被拒绝
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().eq("name; drop", "x").whereSql(),
                "illegal criteria field should throw");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().in("x') or '1'='1", java.util.Arrays.asList(1, 2)).whereSql(),
                "illegal criteria in field should throw");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().isNull("a b"), "illegal criteria isNull field should throw");
    }

    // ---------- last() 严格 + RawSql 授权 ----------

    @Test public void testLastStrictAndRawSql() {
        // 合法尾部片段：仍拼接
        String sql = new QueryCriteria<VerifyDemoEntity>().last("FOR UPDATE").whereSql();
        assertEquals(" FOR UPDATE", sql);

        // 默认严格：分号 / 注释 / 控制字符被拒绝
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("; DROP TABLE users").whereSql(),
                "last with semicolon should throw");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("-- comment").whereSql(),
                "last with comment should throw");
        assertThrows(() -> new QueryCriteria<VerifyDemoEntity>().last("x\nDROP TABLE users").whereSql(),
                "last with newline should throw");

        // RawSql 显式授权：绕过
        String rawSql = new QueryCriteria<VerifyDemoEntity>().last(RawSql.wrap("LIMIT 5; DROP TABLE users")).whereSql();
        assertEquals(" LIMIT 5; DROP TABLE users", rawSql);

        // RawSql 不能为 null / 空 / 纯空白
        assertThrowsRawSql(() -> RawSql.wrap(null), "RawSql wrap(null) should throw");
        assertThrowsRawSql(() -> RawSql.wrap(""), "RawSql wrap(empty) should throw");
        assertThrowsRawSql(() -> RawSql.wrap("   "), "RawSql wrap(blank) should throw");
        // RawSql 显式授权允许控制字符 / 分号（信任边界）
        assertEquals("x\nDROP", RawSql.wrap("x\nDROP").sql());
    }

    @Test public void testRawSqlFactory() {
        RawSql rawSql = SQL.raw("FOR UPDATE");
        assertEquals("FOR UPDATE", rawSql.sql());

        assertThrowsRawSql(() -> SQL.raw("   "), "SQL.raw blank should throw");
    }

    // ------------------------ 辅助断言 ------------------------

    private static void assertValid(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw new IllegalStateException(message + " threw: " + e.getMessage(), e);
        }
    }

    private static void assertValidColumnRef(String ref) {
        assertValid(() -> SqlIdentifierUtils.checkColumnRef(ref), "column ref should be valid: " + ref);
    }

    private static void assertValidTable(String name) {
        assertValid(() -> SqlIdentifierUtils.checkTableName(name), "table name should be valid: " + name);
    }

    private static void assertValidAlias(String alias) {
        assertValid(() -> SqlIdentifierUtils.checkAlias(alias), "alias should be valid: " + alias);
    }

    private static void assertValidColumnName(String name) {
        assertValid(() -> SqlIdentifierUtils.checkColumnName(name), "column name should be valid: " + name);
    }

    private static void assertValidTail(String tail) {
        assertValid(() -> SqlIdentifierUtils.checkTailSql(tail), "tail should be valid: " + tail);
    }

    private static void assertThrowsColumnRef(String ref) {
        assertThrows(() -> SqlIdentifierUtils.checkColumnRef(ref), "column ref should be rejected: " + ref);
    }

    private static void assertThrowsTableName(String name) {
        assertThrows(() -> SqlIdentifierUtils.checkTableName(name), "table name should be rejected: " + name);
    }

    private static void assertThrowsAlias(String alias) {
        assertThrows(() -> SqlIdentifierUtils.checkAlias(alias), "alias should be rejected: " + alias);
    }

    private static void assertThrowsColumnName(String name) {
        assertThrows(() -> SqlIdentifierUtils.checkColumnName(name), "column name should be rejected: " + name);
    }

    private static void assertThrowsTail(String tail) {
        assertThrows(() -> SqlIdentifierUtils.checkTailSql(tail), "tail should be rejected: " + tail);
    }

    private static void assertThrows(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (TinyJdbcException e) {
            if (!e.getMessage().contains("Illegal SQL")) {
                throw new IllegalStateException("unexpected exception message: " + e.getMessage(), e);
            }
            return;
        }
        throw new IllegalStateException(message);
    }

    /**
     * 断言 RawSql 构造器对 null / 空白入参抛出 TinyJdbcException。
     */
    private static void assertThrowsRawSql(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (TinyJdbcException e) {
            if (!e.getMessage().contains("cannot be null or empty")) {
                throw new IllegalStateException("unexpected exception message: " + e.getMessage(), e);
            }
            return;
        }
        throw new IllegalStateException(message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + " expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "");
    }
}
