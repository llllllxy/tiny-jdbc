package org.tinycloud.jdbc.sql;

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
    private String column;
    private List<?> values;
    private JoinType joinType;

    public InCondition(String column, List<?> values, JoinType joinType) {
        this.column = column;
        this.values = values;
        this.joinType = joinType;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder(column).append(" IN (");
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
