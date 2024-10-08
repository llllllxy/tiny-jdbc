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
        Pair<List<String>, String> tableColumn = tableColumnCache.get(clazz);
        if (tableColumn != null) {
            return tableColumn;
        }
        Field[] fields = resolveFields(clazz);
        String primaryKeyColumn = null;
        List<String> columnList = new ArrayList<>();
        for (Field field : fields) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);
            String column;
            if (columnAnnotation == null || StrUtils.isEmpty(columnAnnotation.value())) {
                column = (StrUtils.humpToLine(field.getName()));
            } else {
                column = (columnAnnotation.value());
            }
            columnList.add(column);
            if (idAnnotation != null) {
                primaryKeyColumn = column;
            }
        }
        if (primaryKeyColumn == null || primaryKeyColumn.isEmpty()) {
            throw new TinyJdbcException("Please correctly set the primary key attribute column!");
        }
        tableColumn = new Pair<>(columnList, primaryKeyColumn);
        tableColumnCache.put(clazz, tableColumn);
        return tableColumn;
    }
}
