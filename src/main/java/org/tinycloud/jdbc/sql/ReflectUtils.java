package org.tinycloud.jdbc.sql;


import org.springframework.format.datetime.DateFormatter;
import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.JdbcException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;

/**
 * java反射工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class ReflectUtils {

    /**
     * 校验Entity对象的合法性
     *
     * @param entity 实体类对象
     * @param <T>    泛型
     */
    public static <T> void validateTargetClass(T entity) {
        if (entity == null) {
            throw new JdbcException("SqlGenerator entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new JdbcException("SqlGenerator " + clazz + "no @Table defined");
        }
        String table = tableAnnotation.value();
        if (StringUtils.isEmpty(table)) {
            throw new JdbcException("SqlGenerator " + clazz + "@Table value cannot be null");
        }
        Field[] fields = getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new JdbcException("SqlGenerator " + clazz + " no field defined");
        }
    }

    /**
     * 判断数据是否为null
     *
     * @param clazz      类对象
     * @param filedValue 属性值
     * @return true or false
     */
    public static boolean typeValueIsNotNull(Class<?> clazz, Object filedValue) {
        // String
        if (clazz == java.lang.String.class) {
            if (filedValue == null || filedValue == "") {
                return false;
            }
            // Number
        } else if (clazz == java.lang.Integer.class || clazz == java.lang.Double.class || clazz == java.lang.Float.class
                || clazz == java.lang.Long.class || clazz == java.lang.Short.class
                || clazz == java.math.BigDecimal.class) {
            if (null == filedValue) {
                return false;
            }
            // Date
        } else if (clazz == java.util.Date.class || clazz == java.sql.Timestamp.class || clazz == java.sql.Date.class) {
            if (null == filedValue) {
                return false;
            }
            // Boolean
        } else if (clazz == java.lang.Boolean.class) {
            if (null == filedValue) {
                return false;
            }
            // Byte
        } else if (clazz == java.lang.Byte.class) {
            if (null == filedValue) {
                return false;
            }
        }
        return true;
    }


    /**
     * 创建类实例
     *
     * @param clazz 类对象
     * @return 对象示例
     */
    public static Object createInstance(Class<?> clazz) {
        Object o;
        try {
            o = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("load class " + clazz.getCanonicalName() + " fail");
        }
        return o;
    }

    /**
     * 根据类对象获取其属性列表
     *
     * @param clazz 类对象
     * @return 字段数组
     */
    public static Field[] getFields(Class<?> clazz) {
        List<Field> list = new ArrayList<>();
        // 遍历类及其父类的所有字段并获取属性名称
        while (clazz != null && !"java.lang.Object".equals(clazz.getName())) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                list.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return list.toArray(new Field[0]);
    }


    /**
     * 直接注入属性值
     *
     * @param o          对象
     * @param fieldName  对象属性名
     * @param fieldValue 对象属性值
     */
    public static void injectFieldValue(Object o, String fieldName, Object fieldValue) {
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, fieldValue);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException("there are no field named " + fieldName + " in class " + o.getClass().getName());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("inject field value fail : " + fieldName);
        }
    }

    /**
     * 通过set注入属性值
     *
     * @param o          对象
     * @param fieldName  对象属性名
     * @param fieldValue 对象属性值
     */
    public static void setFieldValue(Object o, String fieldName, Object fieldValue) {
        Method[] declaredMethods = o.getClass().getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            if (declaredMethod.getName().equalsIgnoreCase("set" + capitalize(fieldName))) {
                try {
                    declaredMethod.invoke(o, fieldValue);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    throw new RuntimeException("set field value fail : " + fieldName);
                }
            }
        }
    }

    /**
     * 字符串首字母转换为大写
     *
     * @param str 字符串
     * @return 转换好的字符串
     */
    private static String capitalize(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }


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
