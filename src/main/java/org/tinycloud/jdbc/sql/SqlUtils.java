package org.tinycloud.jdbc.sql;


import org.springframework.util.StringUtils;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.JdbcException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * sql构建工具类
 *
 * @author liuxingyu01
 * @since 2023-07-28-16:49
 **/
public class SqlUtils {

    /**
     * 校验Entity对象的合法性
     *
     * @param entity
     * @param <T>
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
        if (fields == null || fields.length < 0) {
            throw new JdbcException("SqlGenerator " + clazz + " no field defined");
        }
    }

    /**
     * 判断数据是否为null
     *
     * @param type
     * @param filedValue
     * @return
     */
    public static boolean typeValueIsNotNull(Class<?> type, Object filedValue) {
        // String
        if (type == java.lang.String.class) {
            if (filedValue == null || filedValue == "") {
                return false;
            }
            // Number
        } else if (type == java.lang.Integer.class || type == java.lang.Double.class || type == java.lang.Float.class
                || type == java.lang.Long.class || type == java.lang.Short.class
                || type == java.math.BigDecimal.class) {
            if (null == filedValue) {
                return false;
            }
            // Date
        } else if (type == java.util.Date.class || type == java.sql.Timestamp.class || type == java.sql.Date.class) {
            if (null == filedValue) {
                return false;
            }
            // Boolean
        } else if (type == java.lang.Boolean.class) {
            if (null == filedValue) {
                return false;
            }
            // Byte
        } else if (type == java.lang.Byte.class) {
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

}
