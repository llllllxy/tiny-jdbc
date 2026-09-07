package org.tinycloud.jdbc.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * <p>
 * 基于 {@code @Column} 的结果集映射器。
 * </p>
 * <p>
 * 默认的 {@link org.springframework.jdbc.core.BeanPropertyRowMapper} 按「列名转驼峰 → 属性名」匹配，
 * 不识别 {@code @Column}。本类改用 {@link TableParserUtils#resolveColumnToFieldMap} 做「列名 → 字段」
 * 精确映射（{@code @Column.value()} 优先、否则驼峰转下划线），因此自定义 {@code @Column} 也能正确回写。
 * </p>
 * <p>
 * 取值复用 Spring {@link JdbcUtils#getResultSetValue} 按目标属性类型精确提取，BLOB/CLOB、primitive null、
 * Oracle 特殊类型、枚举及 java.time 均能正确转换；赋值复用 {@link BeanWrapperImpl} 并注入
 * {@link DefaultConversionService}，与 {@code BeanPropertyRowMapper} 的类型转换能力保持一致。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public class TableRowMapper<T> implements RowMapper<T> {

    private final Class<T> type;

    private final Map<String, Field> columnToField;

    public TableRowMapper(Class<T> type) {
        this.type = type;
        this.columnToField = TableParserUtils.resolveColumnToFieldMap(type);
    }

    /**
     * 静态工厂方法，与 {@link org.springframework.jdbc.core.BeanPropertyRowMapper#newInstance} 保持一致的调用习惯。
     *
     * @param mappedClass 实体类
     * @param <T>         实体泛型
     * @return 一个基于实体类构建的 {@link TableRowMapper}
     */
    public static <T> TableRowMapper<T> newInstance(Class<T> mappedClass) {
        return new TableRowMapper<>(mappedClass);
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean = BeanUtils.instantiateClass(this.type);
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        // 与 BeanPropertyRowMapper 对齐：注入 DefaultConversionService，使 JDBC 原始值能转换成
        // java.time / UUID / 枚举等目标属性类型（否则 Timestamp->LocalDateTime 等会抛类型转换异常）。
        wrapper.setConversionService(DefaultConversionService.getSharedInstance());
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String column = metaData.getColumnLabel(i);
            if (column == null) {
                column = metaData.getColumnName(i);
            }
            Field field = column == null ? null : this.columnToField.get(column.toLowerCase());
            if (field == null) {
                continue;
            }
            // 按目标属性类型精确取值（复用 Spring JdbcUtils）：正确处理 primitive/wasNull、
            // byte[]/Blob/Clob、Oracle 特殊类型、枚举及 java.time，避免仅依赖 getObject 的裸对象导致转换失败。
            Object value = JdbcUtils.getResultSetValue(rs, i, field.getType());
            if (value == null) {
                // 值为 null 时不 set，让实体保持默认值（对象字段→null，基础类型→默认值），避免类型转换报错
                continue;
            }
            wrapper.setPropertyValue(field.getName(), value);
        }
        return bean;
    }

    @Override
    public String toString() {
        return "TableRowMapper<" + this.type.getName() + ">";
    }
}
