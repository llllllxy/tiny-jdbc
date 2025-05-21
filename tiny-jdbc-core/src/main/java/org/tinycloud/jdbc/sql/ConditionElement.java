package org.tinycloud.jdbc.sql;

import java.util.List;

/**
 * <p>
 *     内部接口：条件元素
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
