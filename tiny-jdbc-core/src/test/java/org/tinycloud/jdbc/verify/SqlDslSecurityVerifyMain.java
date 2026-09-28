package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.FuncBuilder;
import org.tinycloud.jdbc.sql.FuncExpr;
import org.tinycloud.jdbc.sql.Join;
import org.tinycloud.jdbc.sql.RawSql;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.sql.condition.SubQueryCondition;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.sql.enums.SqlJoinType;
import org.tinycloud.jdbc.util.SqlOperatorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * SQL 构造 DSL 的安全加固回归测试。
 * <p>
 * 覆盖 AUDIT_REPORT.md「三、SQL 构造 DSL」全部 15 条，以及同一注入类别下
 * 审计未点名、但由 {@code FuncExpr.raw} 收紧后一并关闭的 FuncBuilder 参数位。
 * </p>
 * <p>本类只做内存 SQL 组装 / 行为断言，不连接数据库。</p>
 */
public class SqlDslSecurityVerifyMain {

    // ==================== 闸门①：操作符白名单 ====================

    // 审计 30：SimpleCondition / and(column, opt, value) 的操作符注入
    @Test
    public void testConditionOperatorInjectionRejected() {
        assertConditionThrows("= 1 OR 1=1");
        assertConditionThrows("= 1) OR (1=1");
        assertConditionThrows("= ?; DROP TABLE t");
        assertConditionThrows("= 'x'");
        assertConditionThrows("");
        assertConditionThrows((String) null);
    }

    private void assertConditionThrows(String operator) {
        try {
            SQL.table("t").select().where(w -> w.and("a.id", operator, 1));
            fail("Expected TinyJdbcException for operator: " + operator);
        } catch (TinyJdbcException e) {
            assertTrue("message should mention operator: " + e.getMessage(),
                    e.getMessage().contains("operator") || e.getMessage().contains("Operator"));
        }
    }

    // 操作符白名单：合法操作符照常工作，且 "not  like" 能规范化
    @Test
    public void testAllowedOperatorsStillWork() {
        assertEquals("SELECT * FROM t WHERE a.id = ?",
                SQL.table("t").select().where(w -> w.and("a.id", "=", 1)).toSql());
        assertEquals("SELECT * FROM t WHERE a.id <=> ?",
                SQL.table("t").select().where(w -> w.and("a.id", "<=>", 1)).toSql());
        // 规范化：去首尾空白 + 折叠内部空白 + 转大写
        assertEquals("SELECT * FROM t WHERE a.name NOT LIKE ?",
                SQL.table("t").select().where(w -> w.and("a.name", "not  like", "%x%")).toSql());
    }

    // 审计 31：Join 的 opt 注入（字符串版 + Lambda 版共用 SimpleCondition）
    @Test
    public void testJoinOperatorInjectionRejected() {
        assertJoinThrows(join -> join.on("t.uid", "u.id OR 1=1", 1));
        assertJoinThrows(join -> join.and("t.uid", "= 1) OR (1=1", 1));
        // Lambda 版走同一校验
        assertJoinThrows(join -> join.and("t.uid", "= 1 OR 1=1", 1));
    }

    private void assertJoinThrows(java.util.function.Consumer<Join> config) {
        try {
            Join join = new Join("u", "u", SqlJoinType.LEFT);
            config.accept(join);
            fail("Expected TinyJdbcException for JOIN operator injection");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("operator") || e.getMessage().contains("Operator"));
        }
    }

    // 白名单外的操作符可用 RawSql 显式授权
    @Test
    public void testOperatorRawSqlEscapeHatch() {
        assertEquals("SELECT * FROM t WHERE a.flag & ?",
                SQL.table("t").select().where(w -> w.and("a.flag", RawSql.wrap("&"), 1)).toSql());
        assertEquals("SELECT * FROM t LEFT JOIN u u ON t.uid & u.id",
                SQL.table("t").select().leftJoin("u", "u")
                        .on("t.uid", RawSql.wrap("&"), new FieldReference("u.id")).toSql());
    }

    @Test
    public void testOperatorUtilsNormalizeAndClassify() {
        assertEquals("NOT LIKE", SqlOperatorUtils.normalize("  not  like "));
        assertEquals("=", SqlOperatorUtils.normalize("="));
        assertTrue(SqlOperatorUtils.isComparisonOperator("like"));
        assertTrue(SqlOperatorUtils.isEqualityOperator(" = "));
        assertTrue(SqlOperatorUtils.isInequalityOperator("<>"));
        assertTrue(SqlOperatorUtils.isInequalityOperator("!="));
        assertFalse(SqlOperatorUtils.isComparisonOperator("OR"));
    }

    // ==================== 闸门②：标识符校验 ====================

    // 审计 27：table(table, alias) 不校验别名
    @Test
    public void testTableAliasInjectionRejected() {
        try {
            SQL.table("t", "u; DROP TABLE x");
            fail("Expected TinyJdbcException for table alias injection");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("alias"));
        }
        // 合法别名与 null 别名不受影响
        assertEquals("SELECT * FROM t u", SQL.table("t", "u").select().toSql());
        assertEquals("SELECT * FROM t", SQL.table("t", null).select().toSql());
    }

    // 审计 28：FuncExpr.as(alias) 不校验别名
    @Test
    public void testFuncExprAliasInjectionRejected() {
        try {
            FuncBuilder.count("a.id").as("c FROM t; DROP TABLE x");
            fail("Expected TinyJdbcException for alias injection");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("alias"));
        }
        assertEquals("SELECT COUNT(a.id) AS c FROM t",
                SQL.table("t").select(FuncBuilder.count("a.id").as("c")).toSql());
    }

    // 审计 35：SubQueryCondition 的 null / 运算符 / 列与运算符组合
    @Test
    public void testSubQueryConditionValidation() {
        // subQuery 为 null
        assertSubQueryThrows(() -> SQL.table("t").select().in("a.id", (SQL<?>) null));
        // EXISTS 不接受列
        assertSubQueryThrows(() -> new SubQueryCondition("a.id", "EXISTS", SQL.table("u").select(), JoinType.AND));
        // IN 必须给列
        assertSubQueryThrows(() -> new SubQueryCondition(null, "IN", SQL.table("u").select(), JoinType.AND));
        // 运算符不在子查询白名单
        assertSubQueryThrows(() -> new SubQueryCondition("a.id", "IN (1) OR (1=1)",
                SQL.table("u").select(), JoinType.AND));
        // 合法用法
        assertEquals("SELECT * FROM t WHERE EXISTS (SELECT * FROM u)",
                SQL.table("t").select().exists(SQL.table("u").select()).toSql());
        assertEquals("SELECT * FROM t WHERE a.id IN (SELECT b.id FROM u)",
                SQL.table("t").select().in("a.id", SQL.table("u").select("b.id")).toSql());
    }

    private void assertSubQueryThrows(Runnable action) {
        try {
            action.run();
            fail("Expected TinyJdbcException for invalid sub-query condition");
        } catch (TinyJdbcException expected) {
            // ok
        }
    }

    // ==================== 闸门③：原始片段信任模型 ====================

    // 审计 29：函数名原样拼接
    @Test
    public void testFunctionNameInjectionRejected() {
        assertFuncNameThrows("COUNT) OR (1=1");
        assertFuncNameThrows("COALESCE(a) FROM t; --");
        assertFuncNameThrows("");
        assertFuncNameThrows(null);
        // 合法函数名照常工作
        assertEquals("SELECT COALESCE(a.id, ?) AS c FROM t",
                SQL.table("t").select(FuncBuilder.func("COALESCE", FuncBuilder.col("a.id"),
                        FuncBuilder.lit("x")).as("c")).toSql());
    }

    private void assertFuncNameThrows(String name) {
        try {
            FuncBuilder.func(name, FuncBuilder.col("a"));
            fail("Expected TinyJdbcException for function name: " + name);
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("function name"));
        }
    }

    // 审计 29 延伸：FuncBuilder 的 String 参数位（col/count/sum/... 约 40 个方法）同样收紧
    @Test
    public void testFuncBuilderStringParamsValidated() {
        String evil = "a.id) FROM t; DROP TABLE x --";
        assertBuilderThrows(() -> FuncBuilder.col(evil));
        assertBuilderThrows(() -> FuncBuilder.count(evil));
        assertBuilderThrows(() -> FuncBuilder.sum(evil));
        assertBuilderThrows(() -> FuncBuilder.max(evil));
        assertBuilderThrows(() -> FuncBuilder.upper(evil));
        assertBuilderThrows(() -> FuncBuilder.countDistinct(evil));
        assertBuilderThrows(() -> FuncBuilder.groupConcatDistinct(evil));
        assertBuilderThrows(() -> FuncBuilder.distinct(evil));
        assertBuilderThrows(() -> FuncBuilder.concat(evil, "b"));
        assertBuilderThrows(() -> FuncBuilder.dateAdd(evil, 1));
        // 非标识符的自由表达式同样被拒绝
        assertBuilderThrows(() -> FuncBuilder.count("a + b"));

        // 合法列引用照常工作
        assertEquals("COUNT(u.name)", FuncBuilder.count("u.name").toSql());
        assertEquals("COUNT(DISTINCT u.name)", FuncBuilder.countDistinct("u.name").toSql());
        assertEquals("SUM(u.amount)", FuncBuilder.sum("u.amount").toSql());
        assertEquals("DATE_ADD(u.created_at, INTERVAL 3 DAY)", FuncBuilder.dateAdd("u.created_at", 3).toSql());
    }

    private void assertBuilderThrows(Runnable action) {
        try {
            action.run();
            fail("Expected TinyJdbcException for non-identifier FuncBuilder argument");
        } catch (TinyJdbcException expected) {
            // ok
        }
    }

    // 自由表达式通过 FuncBuilder.raw(RawSql) 显式授权
    @Test
    public void testFuncBuilderRawEscapeHatch() {
        assertEquals("SELECT price * qty AS total FROM t",
                SQL.table("t").select(FuncBuilder.raw(RawSql.wrap("price * qty")).as("total")).toSql());
        assertEquals("SUM(a || '-' || b)", FuncBuilder.sum(FuncBuilder.raw(RawSql.wrap("a || '-' || b"))).toSql());
    }

    // 审计 33：FuncExpr.of(String) 不再是不受限出口；of(RawSql) 才是
    @Test
    @SuppressWarnings("deprecation")
    public void testFuncExprOfStringIsNoLongerRawEscapeHatch() {
        try {
            FuncExpr.of("a + b");
            fail("Expected TinyJdbcException: FuncExpr.of(String) must validate as column reference");
        } catch (TinyJdbcException expected) {
            // ok
        }
        // 列引用仍可用；自由表达式改用 RawSql
        assertEquals("a.id", FuncExpr.of("a.id").toSql());
        assertEquals("a + b", FuncExpr.of(RawSql.wrap("a + b")).toSql());
    }

    // 审计 32：getParameters 返回只读视图
    @Test
    public void testFuncExprGetParametersIsUnmodifiable() {
        FuncExpr expr = FuncBuilder.count(FuncBuilder.lit(1));
        assertEquals(1, expr.getParameters().size());
        try {
            expr.getParameters().add("injected");
            fail("Expected UnsupportedOperationException on mutating getParameters()");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        // 内部参数未被污染
        assertEquals(1, expr.getParameters().size());
    }

    // ==================== 审计 34：GROUP_CONCAT 分隔符 ====================

    @Test
    public void testGroupConcatSeparatorRenderedAsEscapedLiteral() {
        // 旧实现产出 "GROUP_CONCAT(name, SEPARATOR ,)"：SEPARATOR 前多了逗号，且分隔符未加引号。
        // 按 MySQL/MariaDB 语法，SEPARATOR 前没有逗号（逗号表示「拼接多个表达式」）。
        assertEquals("SELECT GROUP_CONCAT(name SEPARATOR ',') AS n FROM t",
                SQL.table("t").select(FuncBuilder.groupConcat("name", ",").as("n")).toSql());
        // 单引号被双写转义：'''' 即"含一个单引号的字面量"
        assertEquals("SELECT GROUP_CONCAT(name SEPARATOR '''') AS n FROM t",
                SQL.table("t").select(FuncBuilder.groupConcat("name", "'").as("n")).toSql());
        // 反斜杠被双写
        assertEquals("SELECT GROUP_CONCAT(name SEPARATOR '\\\\') AS n FROM t",
                SQL.table("t").select(FuncBuilder.groupConcat("name", "\\").as("n")).toSql());
        // 注入尝试被完整关进字面量内，无法逃逸
        String injected = SQL.table("t").select(FuncBuilder.groupConcat("name", "', (SELECT 1) --").as("n")).toSql();
        assertEquals("SELECT GROUP_CONCAT(name SEPARATOR ''', (SELECT 1) --') AS n FROM t", injected);
    }

    // ==================== 审计 36：空条件组 ====================

    @Test
    public void testEmptyConditionGroupIsSkipped() {
        // 空嵌套块不再渲染成非法的 "()"
        assertEquals("SELECT * FROM t WHERE a = ?",
                SQL.table("t").select().where(w -> w.eq("a", 1).and(g -> { })).toSql());
        assertEquals("SELECT * FROM t WHERE a = ?",
                SQL.table("t").select().where(w -> w.eq("a", 1).or(g -> { })).toSql());
        assertEquals("SELECT * FROM t WHERE a = ?",
                SQL.table("t").select().where(w -> w.eq("a", 1).group(g -> { })).toSql());
        // 纯空条件组：WHERE 整体不输出
        assertEquals("SELECT * FROM t",
                SQL.table("t").select().where(w -> w.and(g -> { })).toSql());
        // 非空嵌套块照常加括号
        assertEquals("SELECT * FROM t WHERE a = ? AND (b = ? OR c = ?)",
                SQL.table("t").select().where(w -> w.eq("a", 1)
                        .and(g -> g.eq("b", 2).or().eq("c", 3))).toSql());
    }

    // 空块不得残留连接符状态（与 criteria 的 OR 泄漏修复同类）
    @Test
    public void testEmptyGroupDoesNotLeakConnectorState() {
        assertEquals("SELECT * FROM t WHERE a = ? AND b = ?",
                SQL.table("t").select().where(w -> {
                    w.eq("a", 1);
                    w.or();
                    w.and(g -> { });
                    w.and();
                    w.eq("b", 2);
                }).toSql());
    }

    // ==================== 审计 37：UNION 自引用 / 重复 ====================

    @Test
    public void testUnionSelfReferenceAndDuplicatesRejected() {
        // 与自身 union
        try {
            SQL<?> a = SQL.table("t").select().limit(1);
            a.union(a);
            fail("Expected TinyJdbcException for self UNION");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("itself"));
        }
        // 同一片段重复入管道
        try {
            SQL<?> a = SQL.table("t").select();
            SQL<?> b = SQL.table("u").select();
            a.union(b);
            a.union(b);
            fail("Expected TinyJdbcException for duplicate UNION segment");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("already part of this UNION pipeline"));
        }
        // 跨管道合并
        try {
            SQL<?> a = SQL.table("t").select();
            SQL<?> b = SQL.table("u").select();
            SQL<?> c = SQL.table("v").select();
            a.union(b);
            c.union(b);
            fail("Expected TinyJdbcException for cross-pipeline UNION");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("another UNION pipeline"));
        }
        // 正常 union 仍工作，且渲染不死循环
        assertEquals("SELECT * FROM t UNION SELECT * FROM u",
                SQL.table("t").select().union(SQL.table("u").select()).toSql());
    }

    // ==================== 审计 38：IN 集合快照 ====================

    @Test
    public void testInConditionSnapshotsCollection() {
        List<Object> values = new ArrayList<>(Arrays.asList(1, 2));
        SQL<?> sq = SQL.table("t").select().where(w -> w.in("a.id", values));
        // 构造后外部增删集合，不应影响已构建的条件
        values.add(3);
        values.remove(0);
        assertEquals("SELECT * FROM t WHERE a.id IN (?, ?)", sq.toSql());
        assertEquals(2, sq.getParameters().size());
        assertEquals(Arrays.asList(1, 2), sq.getParameters());
    }

    // ==================== 审计 39：非 CROSS JOIN 必须有 ON ====================

    @Test
    public void testJoinWithoutOnIsRejectedExceptCross() {
        try {
            SQL.table("t").select().leftJoin("u", "u").toSql();
            fail("Expected TinyJdbcException for LEFT JOIN without ON");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("at least one ON condition"));
        }
        try {
            SQL.table("t").select().innerJoin("u", "u").toSql();
            fail("Expected TinyJdbcException for INNER JOIN without ON");
        } catch (TinyJdbcException expected) {
            // ok
        }
        // CROSS JOIN 允许没有 ON
        assertEquals("SELECT * FROM t CROSS JOIN u u",
                SQL.table("t").select().crossJoin("u", "u").toSql());
    }

    // ==================== 审计 40：limit / offset 负数 ====================

    @Test
    public void testNegativeLimitAndOffsetRejected() {
        try {
            SQL.table("t").select().limit(-1);
            fail("Expected TinyJdbcException for negative limit");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("LIMIT"));
        }
        try {
            SQL.table("t").select().offset(-1);
            fail("Expected TinyJdbcException for negative offset");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("OFFSET"));
        }
        // 0 与正数合法
        assertEquals("SELECT * FROM t LIMIT 0", SQL.table("t").select().limit(0).toSql());
        assertEquals("SELECT * FROM t LIMIT 10 OFFSET 20",
                SQL.table("t").select().limit(10).offset(20).toSql());
    }

    // ==================== 审计 41：eq(col, null) 归一 ====================

    @Test
    public void testNullComparisonNormalizedToIsNull() {
        // eq(col, null) -> IS NULL，且不产生参数
        SQL<?> eqNull = SQL.table("t").select().where(w -> w.eq("a.id", null));
        assertEquals("SELECT * FROM t WHERE a.id IS NULL", eqNull.toSql());
        assertEquals(0, eqNull.getParameters().size());

        // notEq(col, null) -> IS NOT NULL
        assertEquals("SELECT * FROM t WHERE a.id IS NOT NULL",
                SQL.table("t").select().where(w -> w.notEq("a.id", null)).toSql());

        // 通用操作符入口同样归一
        assertEquals("SELECT * FROM t WHERE a.id IS NULL",
                SQL.table("t").select().where(w -> w.and("a.id", "=", null)).toSql());
        assertEquals("SELECT * FROM t WHERE a.id IS NOT NULL",
                SQL.table("t").select().where(w -> w.and("a.id", "!=", null)).toSql());

        // 其它运算符配 null 无意义，直接拒绝
        try {
            SQL.table("t").select().where(w -> w.and("a.id", ">", null));
            fail("Expected TinyJdbcException for '>' with null value");
        } catch (TinyJdbcException e) {
            assertTrue(e.getMessage().contains("null value"));
        }

        // 非 null 值不受影响
        assertEquals("SELECT * FROM t WHERE a.id = ?",
                SQL.table("t").select().where(w -> w.eq("a.id", 1)).toSql());
    }

    // JOIN 的 ON 条件与 WHERE 共用同一实现，null 归一规则一致
    @Test
    public void testJoinNullComparisonNormalizedToIsNull() {
        assertEquals("SELECT * FROM t LEFT JOIN u u ON u.deleted_at IS NULL",
                SQL.table("t").select().leftJoin("u", "u").on("u.deleted_at", "=", null).toSql());
    }

    // ==================== 标识符白名单边界 ====================

    @Test
    public void testIdentifierWhitelistBoundaries() {
        // 合法：ATOM、限定引用、通配
        assertEquals("SELECT u.name FROM t", SQL.table("t").select("u.name").toSql());
        assertEquals("SELECT u.* FROM t", SQL.table("t").select("u.*").toSql());
        assertEquals("SELECT * FROM t", SQL.table("t").select().toSql());
        // 非法：空白 / 分号 / 引号 / 括号 / 注释
        for (String bad : new String[]{"u.name; DROP", "u.na me", "u.name'", "u.name)", "u.name--", "(SELECT 1)"}) {
            try {
                SQL.table("t").select(bad);
                fail("Expected TinyJdbcException for illegal column ref: " + bad);
            } catch (TinyJdbcException expected) {
                // ok
            }
        }
    }
}
