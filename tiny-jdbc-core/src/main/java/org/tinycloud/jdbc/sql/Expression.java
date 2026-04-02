package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * <p>
 * Expression表达式类
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-05-23 10:07
 */
public class Expression {
    private final String expression;
    private String alias;

    /**
     * 私有构造方法，用于创建 Expression 对象。
     *
     * @param expression 要封装的 SQL 表达式字符串
     */
    private Expression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new TinyJdbcException("Expression cannot be null or empty");
        }
        this.expression = expression;
    }

    /**
     * 私有构造方法，用于创建 Expression 对象。
     *
     * @param expression 要封装的 SQL 表达式字符串
     */
    private Expression(String expression, String alias) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new TinyJdbcException("Expression cannot be null or empty");
        }
        if (alias == null || alias.trim().isEmpty()) {
            throw new TinyJdbcException("Alias cannot be null or empty");
        }
        this.expression = expression;
        this.alias = alias;
    }

    /**
     * 为当前表达式设置别名。
     *
     * @param alias 要设置的别名
     * @return 返回当前 Expression 对象，支持链式调用
     */
    public Expression as(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new TinyJdbcException("Alias cannot be null or empty");
        }
        this.alias = alias;
        return this;
    }

    /**
     * 将表达式转换为字符串，若设置了别名则包含别名信息。
     *
     * @return 包含别名的表达式字符串，若未设置别名则返回原始表达式
     */
    @Override
    public String toString() {
        return this.alias != null ? this.expression + " AS " + this.alias : this.expression;
    }

    /**
     * 创建一个 COUNT 聚合函数表达式。
     *
     * @param column 要进行计数的列名
     * @return 包含 COUNT 函数的 Expression 对象
     */
    public static Expression count(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("Column cannot be null or empty");
        }
        return new Expression("COUNT(" + column + ")", "total");
    }

    /**
     * 创建一个 SUM 聚合函数表达式。
     *
     * @param column 要进行求和的列名
     * @return 包含 SUM 函数的 Expression 对象
     */
    public static Expression sum(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("Column cannot be null or empty");
        }
        return new Expression("SUM(" + column + ")", column);
    }

    /**
     * 创建一个 AVG 聚合函数表达式。
     *
     * @param column 要进行求平均值的列名
     * @return 包含 AVG 函数的 Expression 对象
     */
    public static Expression avg(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("Column cannot be null or empty");
        }
        return new Expression("AVG(" + column + ")", column);
    }

    /**
     * 创建一个 MAX 聚合函数表达式。
     *
     * @param column 要进行求最大值的列名
     * @return 包含 MAX 函数的 Expression 对象
     */
    public static Expression max(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("Column cannot be null or empty");
        }
        return new Expression("MAX(" + column + ")", column);
    }

    /**
     * 创建一个 MIN 聚合函数表达式。
     *
     * @param column 要进行求最小值的列名
     * @return 包含 MIN 函数的 Expression 对象
     */
    public static Expression min(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new TinyJdbcException("Column cannot be null or empty");
        }
        return new Expression("MIN(" + column + ")", column);
    }

    /**
     * 创建一个通用的 SQL 表达式对象。
     *
     * @param expression 普通的 SQL 表达式字符串，例如 UCASE(column_name) ，LCASE(column_name)
     * @return 包含指定表达式的 Expression 对象
     */
    public static Expression of(String expression) {
        return new Expression(expression);
    }

    /**
     * 创建一个基于 Lambda 表达式的通用 SQL 表达式对象。
     * 该方法接收一个 TypeFunction 类型的 Lambda 表达式，通过 LambdaUtils 工具类获取对应的数据库列名，
     * 并将其封装为一个 Expression 对象。
     *
     * @param <T>    实体类的类型
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @return 包含 Lambda 表达式对应数据库列名的 Expression 对象
     */
    public static <T> Expression of(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression(column);
    }

    /**
     * 创建一个 COUNT 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 COUNT 函数的 Expression 对象
     */
    public static <T> Expression count(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("COUNT(" + column + ")", "total");
    }

    /**
     * 创建一个 SUM 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 SUM 函数的 Expression 对象
     */
    public static <T> Expression sum(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("SUM(" + column + ")", column);
    }

    /**
     * 创建一个 AVG 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 AVG 函数的 Expression 对象
     */
    public static <T> Expression avg(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("AVG(" + column + ")", column);
    }

    /**
     * 创建一个 MAX 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 MAX 函数的 Expression 对象
     */
    public static <T> Expression max(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("MAX(" + column + ")", column);
    }

    /**
     * 创建一个 MIN 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 MIN 函数的 Expression 对象
     */
    public static <T> Expression min(TypeFunction<T, ?> fields) {
        if (fields == null) {
            throw new TinyJdbcException("Fields cannot be null");
        }
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("MIN(" + column + ")", column);
    }

    /**
     * 创建一个 CASE WHEN 表达式。
     *
     * @return CASE WHEN 表达式构建器
     */
    public static CaseWhenBuilder caseWhen() {
        return new CaseWhenBuilder();
    }

    /**
     * 创建一个 COALESCE 函数表达式。
     *
     * @param expressions 表达式列表
     * @return COALESCE 函数表达式
     */
    public static Expression coalesce(String... expressions) {
        if (expressions == null || expressions.length == 0) {
            throw new TinyJdbcException("At least one expression is required for COALESCE");
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String expr : expressions) {
            joiner.add(expr);
        }
        return new Expression("COALESCE(" + joiner.toString() + ")", expressions[0]);
    }

    /**
     * 创建一个 IFNULL 函数表达式。
     *
     * @param expression  表达式
     * @param replacement 替换值
     * @return IFNULL 函数表达式
     */
    public static Expression ifNull(String expression, String replacement) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new TinyJdbcException("Expression cannot be null or empty");
        }
        if (replacement == null) {
            throw new TinyJdbcException("Replacement cannot be null");
        }
        return new Expression("IFNULL(" + expression + ", " + replacement + ")", expression);
    }

    /**
     * CASE WHEN 表达式构建器
     */
    public static class CaseWhenBuilder {
        private final List<WhenClause> whenClauses = new ArrayList<>();
        private String elseExpression;

        /**
         * 添加 WHEN 条件和结果
         *
         * @param condition 条件表达式
         * @param result    结果表达式
         * @return 当前构建器
         */
        public CaseWhenBuilder when(String condition, String result) {
            if (condition == null || condition.trim().isEmpty()) {
                throw new TinyJdbcException("Condition cannot be null or empty");
            }
            if (result == null || result.trim().isEmpty()) {
                throw new TinyJdbcException("Result cannot be null or empty");
            }
            whenClauses.add(new WhenClause(condition, result));
            return this;
        }

        /**
         * 添加 ELSE 结果
         *
         * @param result 结果表达式
         * @return 当前构建器
         */
        public CaseWhenBuilder otherwise(String result) {
            if (result == null || result.trim().isEmpty()) {
                throw new TinyJdbcException("Result cannot be null or empty");
            }
            this.elseExpression = result;
            return this;
        }

        /**
         * 构建 CASE WHEN 表达式
         *
         * @return Expression 对象
         */
        public Expression build() {
            if (whenClauses.isEmpty()) {
                throw new TinyJdbcException("At least one WHEN clause is required");
            }

            StringBuilder sb = new StringBuilder("CASE");
            for (WhenClause clause : whenClauses) {
                sb.append(" WHEN ").append(clause.condition).append(" THEN ").append(clause.result);
            }
            if (elseExpression != null) {
                sb.append(" ELSE ").append(elseExpression);
            }
            sb.append(" END");

            return new Expression(sb.toString());
        }

        /**
         * WHEN 子句
         */
        private static class WhenClause {
            private final String condition;
            private final String result;

            WhenClause(String condition, String result) {
                this.condition = condition;
                this.result = result;
            }
        }
    }
}
