package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 表信息解析工具类
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-12 14:53
 */
public class TableParserUtils {

    private static final Map<Class<?>, String> tableNameCache = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, Pair<List<String>, String>> tableColumnCache = new ConcurrentHashMap<>(128);


    /**
     * 获取属性列表-Field
     *
     * @param entity 实体类对象
     * @param <T>    泛型
     */
    public static <T> Field[] resolveFields(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("resolveFields entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        return resolveFields(clazz);
    }

    /**
     * 获取属性列表-Field
     *
     * @param clazz 对象类型
     * @param <T>   泛型
     */
    public static <T> Field[] resolveFields(Class<T> clazz) {
        Field[] fields = ReflectUtils.getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new TinyJdbcException("resolveFields " + clazz.getName() + " no field defined!");
        }
        return fields;
    }

    /**
     * 获取表名
     *
     * @param entity 实体类对象
     * @param <T>    泛型
     * @return 表名字符串
     */
    public static <T> String getTableName(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("getTableName entity cannot be null!");
        }
        Class<?> clazz = entity.getClass();
        return getTableName(clazz);
    }

    /**
     * 获取表名
     *
     * @param clazz 实体类类型
     * @param <T>   泛型
     * @return 表名字符串
     */
    public static <T> String getTableName(Class<T> clazz) {
        return ConcurrentHashMapUtils.computeIfAbsent(tableNameCache, clazz, key -> {
            Table tableAnnotation = key.getAnnotation(Table.class);
            if (tableAnnotation == null) {
                throw new TinyJdbcException("getTableName " + key.getName() + " no @Table defined");
            }
            String tableName = tableAnnotation.value();
            if (StrUtils.isEmpty(tableName)) {
                throw new TinyJdbcException("getTableName " + key.getName() + " @Table value cannot be null");
            }
            return tableName;
        });
    }

    /**
     * 获取实体类对应数据库字段名列表和主键字段名
     *
     * @param entity 实体类
     * @param <T>    泛型
     * @return Pair，左数据库字段名列表，右主键字段名
     */
    public static <T> Pair<List<String>, String> getTableColumn(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("getTableColumn entity cannot be null!");
        }
        Class<?> clazz = entity.getClass();
        return getTableColumn(clazz);
    }

    /**
     * 获取实体类对应数据库字段名列表和主键字段名
     *
     * @param clazz 实体类类型
     * @param <T>   泛型
     * @return Pair，左数据库字段名列表，右主键字段名
     */
    public static <T> Pair<List<String>, String> getTableColumn(Class<T> clazz) {
        return ConcurrentHashMapUtils.computeIfAbsent(tableColumnCache, clazz, key -> {
            Field[] fields = resolveFields(key);
            String primaryKeyColumn = null;
            List<String> columnList = new ArrayList<>();
            for (Field field : fields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                Id idAnnotation = field.getAnnotation(Id.class);
                String column;

                // 1. 优先处理 Column 注解的 exist 属性：exist=false 直接跳过
                if (columnAnnotation != null && !columnAnnotation.exist()) {
                    continue;
                }

                // 2. 解析列名（注解value优先，无注解则驼峰转下划线）
                if (columnAnnotation != null && StrUtils.isNotEmpty(columnAnnotation.value())) {
                    column = columnAnnotation.value();
                } else {
                    column = StrUtils.camelToUnderline(field.getName());
                }
                // 3. 加入字段列表（此时 column 一定是有效数据库字段）
                columnList.add(column);

                // 4. 记录主键列（仅当字段存在且有 @Id 注解时）
                if (idAnnotation != null) {
                    primaryKeyColumn = column;
                }
            }

            // 校验主键是否存在
            if (StrUtils.isEmpty(primaryKeyColumn)) {
                throw new TinyJdbcException("Please correctly set the primary key attribute column!");
            }

            return new Pair<>(columnList, primaryKeyColumn);
        });
    }
}
