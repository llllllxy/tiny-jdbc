package org.tinycloud.jdbc.util;

import org.springframework.format.datetime.DateFormatter;

import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;


/**
 * sql工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class SqlUtils {
    /**
     * 替换 sql 中的问号 ？
     *
     * @param sql    sql 内容
     * @param params 参数
     * @return 完整的 sql
     */
    public static String replaceSqlParams(String sql, Object[] params) {
        if (params != null && params.length > 0) {
            for (Object value : params) {
                // null
                if (value == null) {
                    sql = sql.replaceFirst("\\?", "null");
                }
                // number
                else if (value instanceof Number || value instanceof Boolean) {
                    sql = sql.replaceFirst("\\?", value.toString());
                }
                // array
                else if (value.getClass().isArray()
                        || value.getClass() == int[].class
                        || value.getClass() == long[].class
                        || value.getClass() == short[].class
                        || value.getClass() == float[].class
                        || value.getClass() == double[].class) {
                    StringJoiner joiner = new StringJoiner(",");
                    for (int i = 0; i < Array.getLength(value); i++) {
                        joiner.add(String.valueOf(Array.get(value, i)));
                    }
                    sql = sql.replaceFirst("\\?", "[" + joiner + "]");
                }
                // other
                else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("'");

                    String datetimePattern = "yyyy-MM-dd HH:mm:ss";
                    if (value instanceof Date) {
                        DateFormatter dateFormatter = new DateFormatter(datetimePattern);
                        sb.append(dateFormatter.print((Date) value, Locale.getDefault()));
                    } else if (value instanceof LocalDateTime) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datetimePattern);
                        sb.append(((LocalDateTime) value).format(formatter));
                    } else {
                        sb.append(value);
                    }
                    sb.append("'");
                    sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(sb.toString()));
                }
            }
        }
        return sql;
    }
}
