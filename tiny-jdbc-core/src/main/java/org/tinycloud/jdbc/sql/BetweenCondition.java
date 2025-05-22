package org.tinycloud.jdbc.sql;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:03
 */
public class BetweenCondition implements ConditionElement {
    private final String column;
    private final Object value1;
    private final Object value2;
    private final boolean isNot;
    private final JoinType joinType;

    public BetweenCondition(String column, Object value1, Object value2, boolean isNot, JoinType joinType) {
        this.column = column;
        this.value1 = value1;
        this.value2 = value2;
        this.isNot = isNot;
        this.joinType = joinType;
    }

    @Override
    public String toSql() {
        return column + (isNot ? " NOT BETWEEN ? AND ?" : " BETWEEN ? AND ?");
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return Arrays.asList(value1, value2);
    }
}
