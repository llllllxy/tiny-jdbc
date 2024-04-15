package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.Locale;

/**
 * <p>
 * 实体类属性名工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-01 16:16
 */
public final class PropertyNamer {
    private PropertyNamer() {
        // Prevent Instantiation of Static Class
    }

    /**
     * methodName 转 fieldName
     *
     * @param name methodName
     * @return fieldName
     */
    public static String methodToProperty(String name) {
        if (name.startsWith("is")) {
            name = name.substring(2);
        } else if (name.startsWith("get") || name.startsWith("set")) {
            name = name.substring(3);
        } else {
            throw new TinyJdbcException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
        }

        if (name.length() == 1 || name.length() > 1 && !Character.isUpperCase(name.charAt(1))) {
            name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
        }

        return name;
    }

    /**
     * 判断是否是一个getter或settre方法方法
     *
     * @param name 方法名
     * @return true是，false不是
     */
    public static boolean isProperty(String name) {
        return isGetter(name) || isSetter(name);
    }

    /**
     * 判断是否是getter方法
     *
     * @param name 方法名
     * @return true是，false不是
     */
    public static boolean isGetter(String name) {
        return name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2;
    }

    /**
     * 判断是否是setter方法
     *
     * @param name 方法名
     * @return true是，false不是
     */
    public static boolean isSetter(String name) {
        return name.startsWith("set") && name.length() > 3;
    }
}
