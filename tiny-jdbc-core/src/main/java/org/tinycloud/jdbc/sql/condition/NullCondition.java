package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.List;
import java.util.Collections;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:09
 */
public class NullCondition implements ConditionElement {
    private final String column;
    private final boolean isNull;
    private final JoinType joinType;

    public NullCondition(String column, boolean isNull, JoinType joinType) {
        this.column = column;
        this.isNull = isNull;
        this.joinType = joinType;
    }

    @Override
    public String toSql() {
        return column + (isNull ? " IS NULL" : " IS NOT NULL");
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return Collections.emptyList(); // IS NULL/IS NOT NULL不需要参数
    }
}
