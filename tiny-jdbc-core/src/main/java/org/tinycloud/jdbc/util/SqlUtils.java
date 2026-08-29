package org.tinycloud.jdbc.util;

import org.springframework.format.datetime.DateFormatter;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;


/**
 * sql工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class SqlUtils {

    /**
     * 替换 sql 中的问号 ？（用于日志展示完整 SQL，不供执行）
     * <p>
     *     从左到右做 token 扫描，仅对真正的参数占位符 {@code ?} 做替换：
     *     <ul>
     *         <li>识别单引号字符串字面量、双引号/反引号标识符、{@code --} 与 {@code /*}{@code *}{@code /} 注释，跳过其中的 {@code ?}；</li>
     *         <li>字符串/日期值内联时会转义单引号（{@code '} → {@code ''}），避免破坏 SQL 引号；</li>
     *         <li>数组值渲染为 {@code v1, v2, ...}（通常配合 {@code IN (?)} 使用，便于查看 IN 查询的实际值）。</li>
     *     </ul>
     * </p>
     *
     * @param sql    sql 内容
     * @param params 参数
     * @return 完整的 sql（仅用于日志展示）
     */
    public static String replaceSqlParams(String sql, Object[] params) {
        if (sql == null || params == null || params.length == 0) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql.length() + 32);
        int i = 0;
        int paramIndex = 0;
        int len = sql.length();
        while (i < len) {
            char c = sql.charAt(i);
            // 字符串字面量 / 标识符：整段跳过，其中的 ? 不是占位符
            if (c == '\'' || c == '"' || c == '`') {
                int end = findQuoteEnd(sql, i, c);
                sb.append(sql, i, end);
                i = end;
                continue;
            }
            // 行注释 --  （SQL 要求 -- 后跟空白或行尾）
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-'
                    && (i + 2 >= len || Character.isWhitespace(sql.charAt(i + 2)))) {
                int end = sql.indexOf('\n', i);
                if (end < 0) {
                    end = len;
                }
                sb.append(sql, i, end);
                i = end;
                continue;
            }
            // 块注释 /* ... */
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                end = end < 0 ? len : end + 2;
                sb.append(sql, i, end);
                i = end;
                continue;
            }
            // 参数占位符
            if (c == '?') {
                if (paramIndex < params.length) {
                    sb.append(renderValue(params[paramIndex]));
                    paramIndex++;
                } else {
                    sb.append(c);
                }
                i++;
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /**
     * 找到以 {@code start} 开头的引号片段（含结束引号）的结尾下标。
     * 处理 SQL 中 {@code ''} 形式的引号转义。
     */
    private static int findQuoteEnd(String s, int start, char quote) {
        int i = start + 1;
        while (i < s.length()) {
            if (s.charAt(i) == quote) {
                // '...''...' ：两个引号挨着表示转义，跳过
                if (i + 1 < s.length() && s.charAt(i + 1) == quote) {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return s.length();
    }

    /**
     * 把单个参数渲染为适合日志展示的 SQL 字面量。
     */
    private static String renderValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value.getClass().isArray()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (int j = 0; j < Array.getLength(value); j++) {
                joiner.add(renderValue(Array.get(value, j)));
            }
            return joiner.toString();
        }
        String str = formatDateTime(value);
        return "'" + str.replace("'", "''") + "'";
    }

    /**
     * 日期/时间格式化，其它类型取 toString。
     */
    private static String formatDateTime(Object value) {
        String datetimePattern = "yyyy-MM-dd HH:mm:ss";
        if (value instanceof Date) {
            return new DateFormatter(datetimePattern).print((Date) value, Locale.getDefault());
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern(datetimePattern));
        }
        if (value instanceof java.time.LocalDate) {
            return ((java.time.LocalDate) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (value instanceof java.time.LocalTime) {
            return ((java.time.LocalTime) value).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        return String.valueOf(value);
    }
}
