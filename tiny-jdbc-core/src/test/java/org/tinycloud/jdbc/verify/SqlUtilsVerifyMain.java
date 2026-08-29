package org.tinycloud.jdbc.verify;

import org.junit.Test;
import org.tinycloud.jdbc.util.SqlUtils;

import static org.junit.Assert.assertEquals;

/**
 * SqlUtils.replaceSqlParams（日志渲染完整 SQL）的测试。
 * 重点验证：字符串/注释/标识符里的 ? 不被误当占位符、单引号转义、数组渲染。
 */
public class SqlUtilsVerifyMain {

    // 验证：字符串字面量里的 ? 不被替换，只替换真正的占位符
    @Test
    public void testReplaceStringLiteralQuestionMarkNotReplaced() {
        String sql = "SELECT * FROM t WHERE nickname = 'a?b' AND age = ?";
        String rendered = SqlUtils.replaceSqlParams(sql, new Object[]{18});
        assertEquals("SELECT * FROM t WHERE nickname = 'a?b' AND age = 18", rendered);
    }

    // 验证：块注释里的 ? 不被替换
    @Test
    public void testReplaceQuestionMarkInCommentNotReplaced() {
        String sql = "SELECT * FROM t WHERE age = ? /* 'ignored ?' */";
        String rendered = SqlUtils.replaceSqlParams(sql, new Object[]{18});
        assertEquals("SELECT * FROM t WHERE age = 18 /* 'ignored ?' */", rendered);
    }

    // 验证：单引号转义，' 被转成 ''
    @Test
    public void testReplaceSingleQuoteEscaped() {
        String sql = "SELECT * FROM t WHERE name = ?";
        String rendered = SqlUtils.replaceSqlParams(sql, new Object[]{"O'Brien"});
        assertEquals("SELECT * FROM t WHERE name = 'O''Brien'", rendered);
    }

    // 验证：数组渲染为括号逗号列表（便于查看 IN 查询实际值）
    @Test
    public void testReplaceArrayRenderedAsList() {
        String sql = "SELECT * FROM t WHERE id IN (?)";
        String rendered = SqlUtils.replaceSqlParams(sql, new Object[]{new Object[]{1, 2, 3}});
        assertEquals("SELECT * FROM t WHERE id IN (1, 2, 3)", rendered);
    }

    // 验证：数字与 null 的渲染
    @Test
    public void testReplaceNullAndNumber() {
        String sql = "SELECT * FROM t WHERE a = ? AND b = ?";
        String rendered = SqlUtils.replaceSqlParams(sql, new Object[]{20, null});
        assertEquals("SELECT * FROM t WHERE a = 20 AND b = null", rendered);
    }
}
