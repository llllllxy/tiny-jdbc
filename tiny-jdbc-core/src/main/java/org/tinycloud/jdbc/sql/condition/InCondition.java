package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:04
 */
public class InCondition implements ConditionElement {
    private final String column;
    private final List<?> values;
    private final JoinType joinType;
    private final boolean isNot;

    public InCondition(String column, List<?> values, boolean isNot, JoinType joinType) {
        this.column = column;
        this.values = values;
        this.joinType = joinType;
        this.isNot = isNot;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder(column).append((isNot ? " NOT IN (" : " IN ("));
        for (int i = 0; i < values.size(); i++) {
            sb.append("?");
            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return new ArrayList<>(values);
    }
}
