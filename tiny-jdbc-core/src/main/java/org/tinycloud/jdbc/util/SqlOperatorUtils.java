package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * <p>
 * SQL 操作符安全校验工具。
 * </p>
 * <p>
 * 操作符与标识符一样，<b>无法通过参数占位符 {@code ?} 转义</b>，只能依靠白名单校验阻断注入：
 * {@code and("a.id", "= 1 OR 1=1", ...)} 这类输入一旦原样拼接即可改变查询语义。
 * 因此这里采用<b>严格枚举白名单</b>——只有明确列出的操作符才被接受，其余一律拒绝。
 * </p>
 * <p>
 * 校验前会把输入规范化（去除首尾空白、把内部连续空白折叠为单个空格、按
 * {@link Locale#ROOT} 转大写），因此 {@code "not  like"} 与 {@code "NOT LIKE"} 等价。
 * </p>
 * <p>
 * 若确需使用白名单之外的方言运算符（例如某些数据库特有的位运算符），
 * 请通过 {@link org.tinycloud.jdbc.sql.RawSql} 显式授权：
 * {@code and("a.flag", RawSql.wrap("&"), 1)}。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-09-28
 */
public class SqlOperatorUtils {

    /**
     * 「列 运算符 值」形态允许的比较类运算符。
     */
    private static final Set<String> COMPARISON_OPERATORS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "=",
                    "<>",
                    "!=",
                    ">",
                    "<",
                    ">=",
                    "<=",
                    "<=>",
                    "LIKE",
                    "NOT LIKE",
                    "REGEXP",
                    "NOT REGEXP",
                    "RLIKE",
                    "NOT RLIKE",
                    "SOUNDS LIKE"
            )));

    /**
     * 「列 运算符 (子查询)」/「EXISTS (子查询)」形态允许的运算符。
     * <p>其中比较类运算符用于 {@code col = (SELECT ...)} 这类单值子查询比较。</p>
     */
    private static final Set<String> SUB_QUERY_OPERATORS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(
                    "IN",
                    "NOT IN",
                    "EXISTS",
                    "NOT EXISTS",
                    "=",
                    "<>",
                    "!=",
                    ">",
                    "<",
                    ">=",
                    "<="
            )));

    private SqlOperatorUtils() {
    }

    /**
     * 校验并规范化「比较类」操作符（用于 {@code 列 运算符 ?}）。
     *
     * @param operator 调用方传入的操作符
     * @return 规范化后的操作符（大写、空白折叠）
     * @throws TinyJdbcException 操作符为 null/空，或不在白名单内
     */
    public static String checkComparisonOperator(String operator) {
        String normalized = normalize(operator);
        if (normalized == null || !COMPARISON_OPERATORS.contains(normalized)) {
            throw new TinyJdbcException("Illegal SQL comparison operator: '" + operator
                    + "'. Allowed operators are " + COMPARISON_OPERATORS
                    + ". Use RawSql.wrap(\"...\") to explicitly opt out of this whitelist.");
        }
        return normalized;
    }

    /**
     * 校验并规范化「子查询类」操作符（用于 {@code 列 IN (子查询)} / {@code EXISTS (子查询)}）。
     *
     * @param operator 调用方传入的操作符
     * @return 规范化后的操作符（大写、空白折叠）
     * @throws TinyJdbcException 操作符为 null/空，或不在白名单内
     */
    public static String checkSubQueryOperator(String operator) {
        String normalized = normalize(operator);
        if (normalized == null || !SUB_QUERY_OPERATORS.contains(normalized)) {
            throw new TinyJdbcException("Illegal SQL sub-query operator: '" + operator
                    + "'. Allowed operators are " + SUB_QUERY_OPERATORS
                    + ". Use RawSql.wrap(\"...\") to explicitly opt out of this whitelist.");
        }
        return normalized;
    }

    /**
     * 判断是否为白名单内的比较类操作符（不抛异常，供渲染期分支判断使用）。
     *
     * @param operator 操作符
     * @return 命中白名单返回 true
     */
    public static boolean isComparisonOperator(String operator) {
        String normalized = normalize(operator);
        return normalized != null && COMPARISON_OPERATORS.contains(normalized);
    }

    /**
     * 判断是否为白名单内的「相等」类操作符（{@code =}）。
     * <p>用于把 {@code = null} 归一为 {@code IS NULL}。</p>
     *
     * @param operator 操作符
     * @return 是相等操作符返回 true
     */
    public static boolean isEqualityOperator(String operator) {
        return "=".equals(normalize(operator));
    }

    /**
     * 判断是否为白名单内的「不等」类操作符（{@code <>} / {@code !=}）。
     * <p>用于把 {@code <> null} 归一为 {@code IS NOT NULL}。</p>
     *
     * @param operator 操作符
     * @return 是不等操作符返回 true
     */
    public static boolean isInequalityOperator(String operator) {
        String normalized = normalize(operator);
        return "<>".equals(normalized) || "!=".equals(normalized);
    }

    /**
     * 规范化操作符：去除首尾空白、把内部连续空白折叠为单个空格、按 {@link Locale#ROOT} 转大写。
     *
     * @param operator 原始操作符
     * @return 规范化结果；输入为 null 或全空白时返回 null
     */
    public static String normalize(String operator) {
        if (operator == null) {
            return null;
        }
        String trimmed = operator.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
