package org.tinycloud.jdbc.util;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.Table;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 表信息解析工具类。
 * </p>
 * <p>
 * 所有「列名解析 / 属性↔列↔结果映射」统一收敛到 {@link #getTableInfo} 生成的 {@link TableInfo}，
 * 遵循唯一规则：{@code @Column.value()} 优先，否则驼峰转下划线；{@code @Column(exist=false)} 字段不参与结果映射。
 * </p>
 *
 * @author liuxingyu01
 * @since 2024-04-12 14:53
 */
public class TableParserUtils {

    private static final Map<Class<?>, TableInfo> tableInfoCache = new ConcurrentHashMap<>(128);

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
        String tableName = getTableInfo(clazz).getTableName();
        if (StrUtils.isEmpty(tableName)) {
            throw new TinyJdbcException("getTableName " + clazz.getName() + " no @Table defined");
        }
        return tableName;
    }

    /**
     * 获取实体类对应数据库字段名列表和主键字段名。
     * <p>
     * <b>已过时：</b>框架内部请改用 {@link #getTableInfo(Class)} 后直接读取
     * {@link TableInfo#getColumns()} 与 {@link TableInfo#getPrimaryKeyColumn()}。
     * 本方法保留仅为兼容旧调用，行为保持不变。
     * </p>
     *
     * @param entity 实体类
     * @param <T>    泛型
     * @return Pair，左数据库字段名列表，右主键字段名
     * @deprecated 使用 {@link #getTableInfo(Class)} 与 {@link TableInfo} 替代
     */
    @Deprecated
    public static <T> Pair<List<String>, String> getTableColumn(T entity) {
        if (entity == null) {
            throw new TinyJdbcException("getTableColumn entity cannot be null!");
        }
        Class<?> clazz = entity.getClass();
        return getTableColumn(clazz);
    }

    /**
     * 获取实体类对应数据库字段名列表和主键字段名。
     * <p>
     * <b>已过时：</b>框架内部请改用 {@link #getTableInfo(Class)} 后直接读取
     * {@link TableInfo#getColumns()} 与 {@link TableInfo#getPrimaryKeyColumn()}。
     * 本方法保留仅为兼容旧调用，行为保持不变。
     * </p>
     *
     * @param clazz 实体类类型
     * @param <T>   泛型
     * @return Pair，左数据库字段名列表，右主键字段名
     * @deprecated 使用 {@link #getTableInfo(Class)} 与 {@link TableInfo} 替代
     */
    @Deprecated
    public static <T> Pair<List<String>, String> getTableColumn(Class<T> clazz) {
        TableInfo tableInfo = getTableInfo(clazz);
        if (StrUtils.isEmpty(tableInfo.getPrimaryKeyColumn())) {
            throw new TinyJdbcException("Please correctly set the primary key attribute column!");
        }
        return new Pair<>(tableInfo.getColumns(), tableInfo.getPrimaryKeyColumn());
    }

    /**
     * 解析实体字段对应的数据库列名。
     * <p>
     * 列名规则与 {@link #getTableInfo} 保持一致：{@code @Column.value()} 优先，否则驼峰转下划线。
     * </p>
     *
     * @param clazz     实体类类型
     * @param fieldName Java 字段名（驼峰）
     * @return 数据库列名
     */
    public static String resolveColumnName(Class<?> clazz, String fieldName) {
        if (clazz == null || StrUtils.isEmpty(fieldName)) {
            throw new TinyJdbcException("resolveColumnName clazz/fieldName cannot be null or empty");
        }
        TableInfo tableInfo = getTableInfo(clazz);
        String column = tableInfo.getColumn(fieldName);
        if (column == null) {
            throw new TinyJdbcException("resolveColumnName " + clazz.getName() + " no field named " + fieldName);
        }
        return column;
    }

    /**
     * 构建实体「数据库列名(小写) → 属性名」映射，用于结果集映射。
     * <p>
     * 跳过 {@code @Column(exist=false)} 字段；结果来自 {@link #getTableInfo} 的类级缓存。
     * </p>
     *
     * @param clazz 实体类类型
     * @return 列名(小写) → 属性名 映射
     */
    public static Map<String, String> resolveColumnToPropertyMap(Class<?> clazz) {
        return getTableInfo(clazz).getColumnToPropertyMap();
    }

    /**
     * 判断实体字段是否为有效（持久化）字段：字段存在且未标记 {@code @Column(exist=false)}。
     * <p>
     * 字段不存在或 {@code @Column(exist=false)} 均返回 {@code false}；与 {@link TableInfo#isPersistentField(String)}
     * 语义一致，供自动填充等场景用“是否持久化”替代单纯的“字段是否存在”，避免向非持久化字段生成更新列。
     * </p>
     *
     * @param clazz     实体类类型
     * @param fieldName Java 字段名（驼峰）
     * @return true 表示该字段可持久化；字段不存在或 {@code @Column(exist=false)} 时返回 false。
     */
    public static boolean isPersistentField(Class<?> clazz, String fieldName) {
        if (clazz == null || StrUtils.isEmpty(fieldName)) {
            return false;
        }
        return getTableInfo(clazz).isPersistentField(fieldName);
    }

    /**
     * 获取（并按类缓存的）实体表结构元信息。
     *
     * @param clazz 实体类类型
     * @return {@link TableInfo} 元信息
     */
    public static TableInfo getTableInfo(Class<?> clazz) {
        if (clazz == null) {
            throw new TinyJdbcException("getTableInfo clazz cannot be null");
        }
        return ConcurrentHashMapUtils.computeIfAbsent(tableInfoCache, clazz, TableParserUtils::buildTableInfo);
    }

    /**
     * 构建实体表结构元信息（唯一列名规则在此实现一次）。
     */
    private static TableInfo buildTableInfo(Class<?> clazz) {
        Field[] fields = ReflectUtils.getFields(clazz);
        if (fields == null || fields.length == 0) {
            throw new TinyJdbcException("resolveFields " + clazz.getName() + " no field defined!");
        }
        // 表名可选：@Table 缺失或为空时允许解析列名（列名解析不依赖表名），表名存在性由 getTableName 单独校验
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        String tableName = null;
        if (tableAnnotation != null && StrUtils.isNotEmpty(tableAnnotation.value())) {
            tableName = tableAnnotation.value();
        }

        List<Field> allFields = new ArrayList<>(fields.length);
        Map<String, String> fieldToColumn = new HashMap<>(fields.length);
        Map<String, Field> columnToField = new HashMap<>(fields.length);
        Map<String, Field> fieldToField = new HashMap<>(fields.length);
        Set<String> persistentFieldNames = new HashSet<>(fields.length);
        List<String> columns = new ArrayList<>(fields.length);
        String primaryKeyColumn = null;

        for (Field field : fields) {
            allFields.add(field);
            fieldToField.put(field.getName(), field);
            Column columnAnnotation = field.getAnnotation(Column.class);
            Id idAnnotation = field.getAnnotation(Id.class);
            boolean exist = columnAnnotation == null || columnAnnotation.exist();

            // 唯一列名规则：@Column.value() 优先，否则驼峰转下划线
            String column;
            if (columnAnnotation != null && StrUtils.isNotEmpty(columnAnnotation.value())) {
                column = columnAnnotation.value();
            } else {
                column = StrUtils.camelToUnderline(field.getName());
            }
            fieldToColumn.put(field.getName(), column);

            if (exist) {
                columnToField.put(column.toLowerCase(), field);
                persistentFieldNames.add(field.getName());
                columns.add(column);
                if (idAnnotation != null) {
                    if (StrUtils.isNotEmpty(primaryKeyColumn)) {
                        throw new TinyJdbcException("Only one @Id is supported, multiple primary key columns found in class "
                                + clazz.getName() + ": " + primaryKeyColumn + ", " + column);
                    }
                    primaryKeyColumn = column;
                }
            }
        }
        return new TableInfo(clazz, tableName, primaryKeyColumn, allFields, fieldToColumn, columnToField,
                fieldToField, persistentFieldNames, columns);
    }
}
