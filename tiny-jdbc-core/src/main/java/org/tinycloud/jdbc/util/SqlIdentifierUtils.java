package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.regex.Pattern;

/**
 * <p>
 * SQL 标识符安全校验工具。
 * </p>
 * <p>
 * 标识符（表名、列名、别名）与值不同，无法通过参数占位符 {@code ?} 转义，
 * 只能通过「白名单」校验来阻断注入：允许字母/数字/下划线，可选 {@code .} 分段的
 * 限定引用（{@code 别名.列名}、{@code 库名.表名}）以及 {@code 别名.*} / {@code *} 通配。
 * </p>
 * <p>
 * 这里的语义是 <b>默认严格</b>：任何不被上述规则接受的输入（含空白、分号、引号、
 * 注释符、括号等）都会被拒绝并抛出 {@link TinyJdbcException}。若确需传入不受限的
 * 原始 SQL 片段，请使用 {@link org.tinycloud.jdbc.sql.RawSql} 显式包裹以表达信任。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-09-01
 */
public class SqlIdentifierUtils {

    /**
     * 单个标识符原子：字母/下划线开头，后接字母/数字/下划线。
     */
    private static final String ATOM = "[A-Za-z_][A-Za-z0-9_]*";

    /**
     * 列引用：{@code name}、{@code a.b}、{@code a.b.*}、{@code a.*}、{@code *}。
     */
    private static final Pattern COLUMN_REF_PATTERN =
            Pattern.compile("^(\\*|" + ATOM + "(\\." + ATOM + ")*(\\.\\*)?)$");

    /**
     * 表名：{@code t_user}、{@code db.t_user}。不允许通配。
     */
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("^" + ATOM + "(\\." + ATOM + ")*$");

    /**
     * 别名 / 裸列名：仅限一个标识符原子。不允许通配。
     */
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^" + ATOM + "$");

    private SqlIdentifierUtils() {
    }

    /**
     * 校验「列引用」是否合法（用于 SELECT 字段、GROUP BY、ORDER BY、条件字段等）。
     *
     * @param columnRef 列引用
     */
    public static void checkColumnRef(String columnRef) {
        if (columnRef == null || columnRef.trim().isEmpty() || !COLUMN_REF_PATTERN.matcher(columnRef).matches()) {
            throw new TinyJdbcException("Illegal SQL column reference: '" + columnRef + "'");
        }
    }

    /**
     * 校验「表名」是否合法。
     *
     * @param tableName 表名
     */
    public static void checkTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty() || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new TinyJdbcException("Illegal SQL table name: '" + tableName + "'");
        }
    }

    /**
     * 校验「别名」是否合法（用于表别名、派生表别名、FROM 别名）。
     *
     * @param alias 别名
     */
    public static void checkAlias(String alias) {
        if (alias == null || alias.trim().isEmpty() || !ALIAS_PATTERN.matcher(alias).matches()) {
            throw new TinyJdbcException("Illegal SQL alias: '" + alias + "'");
        }
    }

    /**
     * 校验「裸列名」是否合法（用于插入列、SET 列、ON DUPLICATE KEY 列等）。
     *
     * @param columnName 裸列名
     */
    public static void checkColumnName(String columnName) {
        if (columnName == null || columnName.trim().isEmpty() || !ALIAS_PATTERN.matcher(columnName).matches()) {
            throw new TinyJdbcException("Illegal SQL column name: '" + columnName + "'");
        }
    }

    /**
     * 校验 {@code last()} 追加的「尾部 SQL 片段」是否安全。
     * <p>
     * {@code last()} 用于追加 {@code FOR UPDATE}、{@code GROUP BY ...}、{@code LIMIT n} 等
     * 受限的尾部子句。此处拒绝一切可能截断 / 注释 / 拼接语句的字符：
     * 分号、单双引号、反引号、{@code --}、{@code #}、块注释的起止符，以及所有控制字符（换行 / 回车 / 制表等）。
     * </p>
     * <p>
     * 若确需不受限的原始 SQL，请使用 {@code last(RawSql.wrap(...))} 显式授权。
     * </p>
     *
     * @param tailSql 尾部 SQL 片段
     */
    public static void checkTailSql(String tailSql) {
        if (tailSql == null || tailSql.trim().isEmpty()) {
            throw new TinyJdbcException("Illegal SQL tail fragment: 'null or empty'");
        }
        // 拒绝所有控制字符（换行 / 回车 / 制表 / 格式化 / null 等）：尾部子句不应包含任何控制字符。
        for (int i = 0; i < tailSql.length(); i++) {
            char c = tailSql.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new TinyJdbcException("Illegal SQL tail fragment (contains control character): '" + tailSql + "'");
            }
        }
        String lower = tailSql.toLowerCase();
        if (lower.contains(";")
                || lower.indexOf('\'') >= 0
                || lower.indexOf('"') >= 0
                || lower.indexOf('`') >= 0
                || lower.contains("--")
                || lower.contains("#")
                || lower.contains("/*")
                || lower.contains("*/")) {
            throw new TinyJdbcException("Illegal SQL tail fragment (contains breaking characters): '" + tailSql + "'");
        }
    }
}
