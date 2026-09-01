package org.tinycloud.jdbc.util;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 实体类对应的表结构元信息，按类缓存一次。
 * </p>
 * <p>
 * 统一承载：表名、主键列、属性(fieldName→column)、列(column→field) 双向映射、按字段名的字段元信息、
 * 有效字段(fieldName)集合、有效列名列表。由 {@link TableParserUtils#getTableInfo} 统一构建与缓存，
 * 供 SQL 生成、结果映射、填充、Lambda 解析复用，保证「属性 ↔ 列 ↔ 结果」映射只有一套规则、一处实现。
 * </p>
 * <p>
 * 本类字段均不可变（只读），不对外暴露可变缓存；外部统一经 {@link TableParserUtils} 的方法访问。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public final class TableInfo {

    private final Class<?> entityClass;
    private final String tableName;
    private final String primaryKeyColumn;
    private final List<Field> allFields;
    private final Map<String, String> fieldToColumn;
    private final Map<String, Field> columnToField;
    private final Map<String, Field> fieldToField;
    private final Set<String> persistentFieldNames;
    private final List<String> columns;

    TableInfo(Class<?> entityClass, String tableName, String primaryKeyColumn,
              List<Field> allFields, Map<String, String> fieldToColumn,
              Map<String, Field> columnToField, Map<String, Field> fieldToField,
              Set<String> persistentFieldNames, List<String> columns) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.primaryKeyColumn = primaryKeyColumn;
        this.allFields = Collections.unmodifiableList(allFields);
        this.fieldToColumn = Collections.unmodifiableMap(fieldToColumn);
        this.columnToField = Collections.unmodifiableMap(columnToField);
        this.fieldToField = Collections.unmodifiableMap(fieldToField);
        this.persistentFieldNames = Collections.unmodifiableSet(persistentFieldNames);
        this.columns = Collections.unmodifiableList(columns);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * 主键列；若实体未声明 {@code @Id} 则为 null。
     */
    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    /**
     * 实体全部字段（含 {@code @Column(exist=false)} 字段）。
     */
    public List<Field> getFields() {
        return allFields;
    }

    /**
     * 有效字段（排除 {@code @Column(exist=false)}）的列名列表，按字段声明顺序。
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * 字段名 → 列名（含 {@code @Column(exist=false)} 字段）；字段不存在返回 null。
     */
    public String getColumn(String fieldName) {
        return fieldToColumn.get(fieldName);
    }

    /**
     * 字段名 → 字段（含 {@code @Column(exist=false)} 字段）；字段不存在返回 null。
     * <p>用于按字段名查找字段元信息（如 Lambda 解析时校验 {@code @Column(exist=false)}），
     * 是「按字段名」统一、唯一的字段访问入口。</p>
     */
    public Field getField(String fieldName) {
        return fieldName == null ? null : fieldToField.get(fieldName);
    }

    /**
     * 字段是否为有效（持久化）字段：字段存在且未标记 {@code @Column(exist=false)}。
     * 字段不存在或 {@code @Column(exist=false)} 均返回 false。
     */
    public boolean isPersistentField(String fieldName) {
        return fieldName != null && persistentFieldNames.contains(fieldName);
    }

    /**
     * 列名(忽略大小写) → 字段；仅有效字段，不存在返回 null。
     */
    public Field getFieldByColumn(String columnName) {
        return columnName == null ? null : columnToField.get(columnName.toLowerCase());
    }

    /**
     * 用于结果集映射的「列名(小写) → 属性名」只读映射。
     */
    public Map<String, String> getColumnToPropertyMap() {
        Map<String, String> map = new HashMap<>(columnToField.size());
        for (Map.Entry<String, Field> entry : columnToField.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getName());
        }
        return map;
    }
}
