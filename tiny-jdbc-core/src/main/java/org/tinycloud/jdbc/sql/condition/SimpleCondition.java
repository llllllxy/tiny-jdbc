package org.tinycloud.jdbc.sql.condition;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.sql.FieldReference;
import org.tinycloud.jdbc.sql.RawSql;
import org.tinycloud.jdbc.sql.enums.JoinType;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;
import org.tinycloud.jdbc.util.SqlOperatorUtils;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 *     条件：简单条件（=、!=、> 、< 、>=、<=、like）
 * </p>
 * <p>
 *     操作符经 {@link SqlOperatorUtils} 白名单校验；值为 {@code null} 时，
 *     {@code =} 归一为 {@code IS NULL}、{@code <>} / {@code !=} 归一为 {@code IS NOT NULL}
 *     （避免生成恒不匹配的 {@code = ?}）。确需白名单外的方言运算符时，用
 *     {@link RawSql} 重载显式授权。
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

    /**
     * 是否为「空值判断」形态：值为 null 且操作符可归一（= → IS NULL，&lt;&gt;/!= → IS NOT NULL）。
     */
    private final boolean nullCheck;

    /**
     * {@code nullCheck} 为 true 时，是否为 {@code IS NOT NULL}（否则为 {@code IS NULL}）。
     */
    private final boolean notNull;

    /**
     * 构造简单条件。操作符必须命中 {@link SqlOperatorUtils} 白名单。
     *
     * @param column   列引用
     * @param operator 比较操作符（白名单内）
     * @param value    值；为 null 时按 {@code = null} / {@code <> null} 归一为 IS NULL / IS NOT NULL
     * @param joinType 与其它条件的连接符
     */
    public SimpleCondition(String column, String operator, Object value, JoinType joinType) {
        this(column, SqlOperatorUtils.checkComparisonOperator(operator), value, joinType, false);
    }

    /**
     * 构造简单条件，并用 {@link RawSql} 显式授权操作符，跳过白名单校验。
     *
     * @param column   列引用
     * @param operator 受信任的操作符片段
     * @param value    值
     * @param joinType 与其它条件的连接符
     */
    public SimpleCondition(String column, RawSql operator, Object value, JoinType joinType) {
        this(column, requireOperator(operator), value, joinType, true);
    }

    private SimpleCondition(String column, String operator, Object value, JoinType joinType, boolean trustOperator) {
        SqlIdentifierUtils.checkColumnRef(column);
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.joinType = joinType;

        // 值为 null 时归一：= null 恒不匹配，改为 IS NULL；<> / != null 改为 IS NOT NULL。
        if (value == null) {
            if (SqlOperatorUtils.isEqualityOperator(operator)) {
                this.nullCheck = true;
                this.notNull = false;
                return;
            }
            if (SqlOperatorUtils.isInequalityOperator(operator)) {
                this.nullCheck = true;
                this.notNull = true;
                return;
            }
            throw new TinyJdbcException("Operator '" + operator + "' cannot be used with a null value on column '"
                    + column + "'. Use isNull() / isNotNull(), or pass a non-null value.");
        }
        this.nullCheck = false;
        this.notNull = false;
    }

    private static String requireOperator(RawSql operator) {
        if (operator == null) {
            throw new TinyJdbcException("RawSql operator cannot be null.");
        }
        return operator.sql();
    }

    @Override
    public String toSql() {
        if (nullCheck) {
            return column + (notNull ? " IS NOT NULL" : " IS NULL");
        }
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
        // IS NULL / IS NOT NULL 不携带参数
        if (nullCheck) {
            return Collections.emptyList();
        }
        if (value instanceof FieldReference) {
            return Collections.emptyList();
        }
        return Collections.singletonList(value);
    }
}
