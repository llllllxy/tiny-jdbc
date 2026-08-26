package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.sql.enums.JoinType;

import java.util.List;

/**
 * <p>
 *     条件：子查询条件（IN / NOT IN / = EXISTS / NOT EXISTS）。
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public class SubQueryCondition implements ConditionElement {
    private final String column;
    private final String operator;
    private final SQL<?> subQuery;
    private final JoinType joinType;

    /**
     * @param column   列名，EXISTS / NOT EXISTS 语义下可为 null
     * @param operator 连接符：IN / NOT IN / = / EXISTS / NOT EXISTS
     * @param subQuery 子查询
     * @param joinType 与其它条件的连接符
     */
    public SubQueryCondition(String column, String operator, SQL<?> subQuery, JoinType joinType) {
        this.column = column;
        this.operator = operator;
        this.subQuery = subQuery;
        this.joinType = joinType;
    }

    @Override
    public String toSql() {
        String subSql = subQuery.toSql();
        if (column == null) {
            return operator + " (" + subSql + ")";
        }
        return column + " " + operator + " (" + subSql + ")";
    }

    @Override
    public JoinType getJoinType() {
        return joinType;
    }

    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }
}
