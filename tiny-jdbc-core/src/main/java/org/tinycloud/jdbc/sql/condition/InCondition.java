package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 *     条件：IN (?,?) 或子查询 IN (SELECT ...)
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-21 14:04
 */
public class InCondition implements ConditionElement {
    private final String column;
    private final Collection<?> values;
    private final SQL<?> subQuery;
    private final JoinType joinType;
    private final boolean isNot;

    public InCondition(String column, Collection<?> values, boolean isNot, JoinType joinType) {
        if (values == null || values.isEmpty()) {
            throw new TinyJdbcException("The values of IN/NOT IN condition cannot be null or empty, column: " + column);
        }
        this.column = column;
        this.values = values;
        this.subQuery = null;
        this.joinType = joinType;
        this.isNot = isNot;
    }

    public InCondition(String column, SQL<?> subQuery, boolean isNot, JoinType joinType) {
        if (subQuery == null) {
            throw new TinyJdbcException("The sub query of IN/NOT IN condition cannot be null, column: " + column);
        }
        this.column = column;
        this.values = null;
        this.subQuery = subQuery;
        this.joinType = joinType;
        this.isNot = isNot;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder(column);
        if (subQuery != null) {
            sb.append(isNot ? " NOT IN (" : " IN (").append(subQuery.toSql()).append(")");
            return sb.toString();
        }
        sb.append(isNot ? " NOT IN (" : " IN (");
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
        if (subQuery != null) {
            return subQuery.getParameters();
        }
        return new ArrayList<>(values);
    }
}
