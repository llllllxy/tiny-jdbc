package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.regex.Pattern;

/**
 * sql注入检测工具类
 *
 * @author liuxingyu01
 * @since 2023-08-05
 **/
public class SqlInjectUtils {
    /**
     * SQL语法检查正则：符合两个关键字（有先后顺序）才算匹配
     */
    private static final Pattern SQL_SYNTAX_PATTERN = Pattern.compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)" +
            "\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*(like|=|>|<|in|between|is|not|exists)", Pattern.CASE_INSENSITIVE);
    /**
     * 使用'、;或注释截断SQL检查正则
     */
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("'.*(or|union|--|#|/\\*|;)", Pattern.CASE_INSENSITIVE);

    /**
     * 检查参数是否存在 SQL 注入
     *
     * @param value 检查参数
     * @return true 非法， false 合法
     */
    public static boolean check(String value) {
        if (value == null) {
            throw new TinyJdbcException("所传入的SQL参数不能为空！");
        }
        // 处理是否包含SQL注释字符 || 检查是否包含SQL注入敏感字符
        return SQL_COMMENT_PATTERN.matcher(value).find() || SQL_SYNTAX_PATTERN.matcher(value).find();
    }

    /**
     * 刪除字段转义符单引号双引号
     *
     * @param text 待处理字段
     * @return 处理后的字段
     */
    public static String removeEscapeCharacter(String text) {
        if (text == null) {
            throw new TinyJdbcException("所传入参数不能为空！");
        }
        return text.replaceAll("\"", "").replaceAll("'", "");
    }
}
