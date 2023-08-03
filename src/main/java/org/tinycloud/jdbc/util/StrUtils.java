package org.tinycloud.jdbc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 字符串工具类
 *
 * @author liuxingyu01
 * @since 2023-08-03
 **/
public class StrUtils {
    private static final Pattern humpPattern = Pattern.compile("[A-Z]");
    private static final Pattern linePattern = Pattern.compile("_(\\w)");

    /**
     * 驼峰转下划线 humpToLine("helloWorld") = "hello_world"
     *
     * @param str 字符串
     * @return 字符串
     */
    public static String humpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 下划线转驼峰 lineToHump("hello_world") = "helloWorld"
     *
     * @param str 字符串
     * @return 字符串
     */
    public static String lineToHump(String str) {
        if (null == str || "".equals(str)) {
            return str;
        }
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);

        str = sb.toString();
        str = str.substring(0, 1).toUpperCase() + str.substring(1);

        return str;
    }
}
