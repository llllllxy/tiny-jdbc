package org.tinycloud.jdbc.fill;

import org.tinycloud.jdbc.criteria.update.LambdaUpdateCriteria;
import org.tinycloud.jdbc.criteria.update.UpdateCriteria;

/**
 * <p>
 * 实体字段自动填充处理器
 * </p>
 * <p>
 * 支持在 INSERT、UPDATE 场景下对实体对象进行统一字段填充。
 * 例如：创建人、创建时间、更新人、更新时间等审计字段。
 * </p>
 *
 * @author liuxingyu01
 * @since 2026-04-18
 */
public interface MetaObjectHandler {

    /**
     * 新增时自动填充
     *
     * @param metaObject 填充上下文
     */
    default void insertFill(FillMetaObject metaObject) {
        // 默认不处理
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 填充上下文
     */
    default void updateFill(FillMetaObject metaObject) {
        // 默认不处理
    }

    /**
     * 仅条件构造器更新时自动填充（字符串字段版）
     *
     * @param criteria    更新条件构造器
     * @param entityClass 实体类型
     * @param <T>         实体泛型
     */
    default <T> void updateCriteriaFill(UpdateCriteria<T> criteria, Class<T> entityClass) {
        // 默认不处理
    }

    /**
     * 仅条件构造器更新时自动填充（Lambda 字段版）
     *
     * @param criteria    更新条件构造器
     * @param entityClass 实体类型
     * @param <T>         实体泛型
     */
    default <T> void updateLambdaCriteriaFill(LambdaUpdateCriteria<T> criteria, Class<T> entityClass) {
        // 默认不处理
    }
}
