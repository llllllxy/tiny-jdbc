package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.SQL;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;
import org.tinycloud.jdbc.util.SqlOperatorUtils;

import java.util.List;

/**
 * <p>
 *     条件：子查询条件（IN / NOT IN / = EXISTS / NOT EXISTS）。
 * </p>
 * <p>
 *     操作符经 {@link SqlOperatorUtils} 白名单校验，并校验「列」与「操作符」的组合是否合法：
 *     {@code EXISTS} / {@code NOT EXISTS} 不接受列（列必须为 null），其余操作符必须提供列。
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
     * 构造子查询条件。操作符必须命中 {@link SqlOperatorUtils} 子查询白名单，
     * 且「列」与「操作符」的组合必须自洽（{@code EXISTS} / {@code NOT EXISTS} 不接受列，其余必须带列）。
     * <p>子查询操作符无需逃生口：白名单已覆盖 IN / NOT IN / EXISTS / NOT EXISTS 与比较符。</p>
     *
     * @param column   列名；{@code EXISTS} / {@code NOT EXISTS} 语义下必须为 null
     * @param operator 连接符：IN / NOT IN / EXISTS / NOT EXISTS / 比较符
     * @param subQuery 子查询，不能为 null
     * @param joinType 与其它条件的连接符
     */
    public SubQueryCondition(String column, String operator, SQL<?> subQuery, JoinType joinType) {
        if (subQuery == null) {
            throw new TinyJdbcException("The sub query of sub-query condition cannot be null, column: " + column);
        }
        String normalized = SqlOperatorUtils.checkSubQueryOperator(operator);
        boolean existsLike = "EXISTS".equals(normalized) || "NOT EXISTS".equals(normalized);
        if (existsLike) {
            if (column != null) {
                throw new TinyJdbcException("EXISTS / NOT EXISTS cannot be combined with a column, but got: '"
                        + column + "'");
            }
        } else {
            if (column == null) {
                throw new TinyJdbcException("Operator '" + normalized + "' requires a column name.");
            }
            SqlIdentifierUtils.checkColumnRef(column);
        }
        this.column = column;
        this.operator = normalized;
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
