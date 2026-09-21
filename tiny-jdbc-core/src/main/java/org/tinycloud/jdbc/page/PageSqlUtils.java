package org.tinycloud.jdbc.page;

/**
 * 分页 SQL 文本处理工具。
 */
final class PageSqlUtils {

    private PageSqlUtils() {
    }

    /**
     * 查找顶层查询的 SELECT 关键字起始位置。
     * <p>会跳过单引号字符串、双引号标识符、行/块注释及 CTE 子查询中的 SELECT。</p>
     *
     * @param sql 原始查询 SQL
     * @return 顶层 SELECT 的起始下标
     * @throws IllegalArgumentException 未找到顶层 SELECT 时抛出
     */
    static int findTopLevelSelect(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL cannot be null");
        }
        int depth = 0;
        int length = sql.length();
        for (int i = 0; i < length; i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                i = skipQuoted(sql, i, '\'');
            } else if (current == '"') {
                i = skipQuoted(sql, i, '"');
            } else if (current == '-' && i + 1 < length && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i + 2);
            } else if (current == '/' && i + 1 < length && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i + 2);
            } else if (current == '(') {
                depth++;
            } else if (current == ')') {
                if (depth > 0) {
                    depth--;
                }
            } else if (depth == 0 && isSelectAt(sql, i)) {
                return i;
            }
        }
        throw new IllegalArgumentException("SQL must contain a top-level SELECT statement");
    }

    private static int skipQuoted(String sql, int start, char quote) {
        for (int i = start + 1; i < sql.length(); i++) {
            if (sql.charAt(i) == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    i++;
                } else {
                    return i;
                }
            }
        }
        return sql.length() - 1;
    }

    private static int skipLineComment(String sql, int start) {
        for (int i = start; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\n' || current == '\r') {
                return i;
            }
        }
        return sql.length() - 1;
    }

    private static int skipBlockComment(String sql, int start) {
        for (int i = start; i + 1 < sql.length(); i++) {
            if (sql.charAt(i) == '*' && sql.charAt(i + 1) == '/') {
                return i + 1;
            }
        }
        return sql.length() - 1;
    }

    private static boolean isSelectAt(String sql, int index) {
        int end = index + 6;
        if (end > sql.length() || !sql.regionMatches(true, index, "SELECT", 0, 6)) {
            return false;
        }
        return (index == 0 || !isIdentifierPart(sql.charAt(index - 1)))
                && (end == sql.length() || !isIdentifierPart(sql.charAt(end)));
    }

    private static boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character) || character == '_' || character == '$';
    }
}
