package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     条件：简单条件（=、!=、> 、< 、>=、<=、like）
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:03
 */
public class SimpleCondition implements ConditionElement {
    private final String column;
    private final String operator;
    private final Object value;
    private final JoinType joinType;

    public SimpleCondition(String column, String operator, Object value, JoinType joinType) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.joinType = joinType;
    }

    @Override
    public String toSql() {
        return column + " " + operator + " ?";
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return Collections.singletonList(value);
    }
}
