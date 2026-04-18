package org.tinycloud.jdbc.fill;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.ReflectUtils;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>
 * 自动填充上下文对象
 * </p>
 * <p>
 * 用于包装实体对象，提供字段读取、写入与“按规则填充”的便捷方法。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public class FillMetaObject {

    private final Object entity;

    public FillMetaObject(Object entity) {
        if (entity == null) {
            throw new TinyJdbcException("fill meta object entity cannot be null");
        }
        this.entity = entity;
    }

    /**
     * 获取原始实体对象
     */
    public Object getEntity() {
        return entity;
    }

    /**
     * 获取字段值
     */
    public Object getValue(String fieldName) {
        try {
            return ReflectUtils.getFieldValue(entity, fieldName);
        } catch (Exception e) {
            throw new TinyJdbcException("get field value failed: " + fieldName, e);
        }
    }

    /**
     * 设置字段值
     */
    public void setValue(String fieldName, Object value) {
        try {
            ReflectUtils.setFieldValue(entity, fieldName, value);
        } catch (Exception e) {
            throw new TinyJdbcException("set field value failed: " + fieldName, e);
        }

    }

    /**
     * 新增场景严格填充：仅当当前字段值为 null 时才写入
     */
    public void strictInsertFill(String fieldName, Supplier<?> valueSupplier) {
        if (this.getValue(fieldName) == null) {
            this.setValue(fieldName, valueSupplier.get());
        }
    }

    /**
     * 更新场景严格填充：仅当字段存在并且 supplier 返回值非 null 时写入
     */
    public void strictUpdateFill(String fieldName, Supplier<?> valueSupplier) {
        Object value = valueSupplier.get();
        if (value != null) {
            this.setValue(fieldName, value);
        }
    }

    /**
     * 新增场景填充：字段为空才填充（别名方法，语义更直观）
     */
    public void fillIfNull(String fieldName, Supplier<?> valueSupplier) {
        this.strictInsertFill(fieldName, valueSupplier);
    }

    /**
     * 更新场景覆盖填充：只要有值就覆盖
     */
    public void fillOverride(String fieldName, Supplier<?> valueSupplier) {
        this.strictUpdateFill(fieldName, valueSupplier);
    }
}
