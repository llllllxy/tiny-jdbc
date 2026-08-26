package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.FieldReference;
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
        // value 若是列引用，则原样拼接（不参数化），实现列到列比较；否则输出占位符 ?
        String v = value instanceof FieldReference ? ((FieldReference) value).getColumn() : "?";
        return column + " " + operator + " " + v;
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        if (value instanceof FieldReference) {
            return Collections.emptyList();
        }
        return Collections.singletonList(value);
    }
}
