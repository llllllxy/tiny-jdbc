package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;

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

    private static final Map<Class<?>, String> tableNameCache = new ConcurrentHashMap<>(256);

    private static final Map<Class<?>, Pair<List<String>, String>> tableColumnCache = new ConcurrentHashMap<>(256);


    /**
     * 获取字段列表-根据
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
     * 获取字段列表-根据
     *
     * @param clazz 对象类型
     */
    public static <T> Field[] resolveFields(Class<?> clazz) {
        Field[] fields = ReflectUtils.getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new TinyJdbcException("resolveFields " + clazz.getName() + " no field defined");
        }
        return fields;
    }


    public static <T> String getTableName(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("getTableName entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        return getTableName(clazz);
    }

    public static <T> String getTableName(Class<T> clazz) {
        String tableName = tableNameCache.get(clazz);
        if (StrUtils.isNotEmpty(tableName)) {
            return tableName;
        }
        Table tableAnnotation = (Table) clazz.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            throw new TinyJdbcException("getTableName " + clazz.getName() + "no @Table defined");
        }
        tableName = tableAnnotation.value();
        if (StrUtils.isEmpty(tableName)) {
            throw new TinyJdbcException("getTableName " + clazz.getName() + "@Table value cannot be null");
        }
        tableNameCache.put(clazz, tableName);
        return tableName;
    }

    public static <T> Pair<List<String>, String> getTableColumn(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("getTableColumn entity cannot be null");
        }
        Class<?> clazz = entity.getClass();
        return getTableColumn(clazz);
    }

    public static <T> Pair<List<String>, String> getTableColumn(Class<T> clazz) {
        Pair<List<String>, String> tableColumn = tableColumnCache.get(clazz);
        if (tableColumn != null) {
            return tableColumn;
        }
        Field[] fields = resolveFields(clazz);
        String primaryKeyColumn = "";
        List<String> columnList = new ArrayList<>();
        for (Field field : fields) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                continue;
            }
            columnList.add(columnAnnotation.value());
            if (columnAnnotation.primaryKey()) {
                primaryKeyColumn = columnAnnotation.value();
            }
        }
        tableColumn = new Pair<>(columnList, primaryKeyColumn);
        tableColumnCache.put(clazz, tableColumn);
        return tableColumn;
    }
}
