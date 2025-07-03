package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.List;

/**
 * <p>
 *     条件：条件元素接口声明
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:02
 */
public interface ConditionElement {
    String toSql();

    JoinType getJoinType();

    List<Object> getParameters();
}
