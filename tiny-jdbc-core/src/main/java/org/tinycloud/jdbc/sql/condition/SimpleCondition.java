package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *     内部类：简单条件
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:03
 */
public class SimpleCondition implements ConditionElement {
    private String column;
    private String operator;
    private Object value;
    private JoinType joinType;

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
        return Arrays.asList(value);
    }
}
