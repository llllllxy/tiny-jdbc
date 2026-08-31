package org.tinycloud.jdbc.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.jdbc.core.RowMapper;

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
 * 不识别 {@code @Column}。本类改用 {@link TableParserUtils#resolveColumnToPropertyMap} 做「列名 → 属性名」
 * 精确映射（{@code @Column.value()} 优先、否则驼峰转下划线），因此自定义 {@code @Column} 也能正确回写。
 * </p>
 * <p>
 * 赋值仍复用 Spring {@link BeanWrapperImpl}，与 {@code BeanPropertyRowMapper} 的类型转换能力保持一致
 * （支持日期时间、UUID、枚举等），且对属性的 setter 依赖与原先相同，不引入新限制。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public class TableRowMapper<T> implements RowMapper<T> {

    private final Class<T> type;

    private final Map<String, String> columnToProperty;

    public TableRowMapper(Class<T> type) {
        this.type = type;
        this.columnToProperty = TableParserUtils.resolveColumnToPropertyMap(type);
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
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String column = metaData.getColumnLabel(i);
            if (column == null) {
                column = metaData.getColumnName(i);
            }
            String property = column == null ? null : this.columnToProperty.get(column.toLowerCase());
            if (property == null) {
                continue;
            }
            Object value = rs.getObject(i);
            if (value == null) {
                // 值为 null 时不 set，让实体保持默认值（对象字段→null，基础类型→默认值），避免类型转换报错
                continue;
            }
            wrapper.setPropertyValue(property, value);
        }
        return bean;
    }

    @Override
    public String toString() {
        return "TableRowMapper<" + this.type.getName() + ">";
    }
}
