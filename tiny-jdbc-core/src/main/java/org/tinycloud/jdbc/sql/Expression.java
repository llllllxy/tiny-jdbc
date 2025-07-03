package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.LambdaUtils;

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
        this.expression = expression;
    }


    /**
     * 为当前表达式设置别名。
     *
     * @param alias 要设置的别名
     * @return 返回当前 Expression 对象，支持链式调用
     */
    public Expression as(String alias) {
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
        return new Expression("COUNT(" + column + ")");
    }

    /**
     * 创建一个 SUM 聚合函数表达式。
     *
     * @param column 要进行求和的列名
     * @return 包含 SUM 函数的 Expression 对象
     */
    public static Expression sum(String column) {
        return new Expression("SUM(" + column + ")");
    }

    /**
     * 创建一个 AVG 聚合函数表达式。
     *
     * @param column 要进行求平均值的列名
     * @return 包含 AVG 函数的 Expression 对象
     */
    public static Expression avg(String column) {
        return new Expression("AVG(" + column + ")");
    }

    /**
     * 创建一个 MAX 聚合函数表达式。
     *
     * @param column 要进行求最大值的列名
     * @return 包含 MAX 函数的 Expression 对象
     */
    public static Expression max(String column) {
        return new Expression("MAX(" + column + ")");
    }

    /**
     * 创建一个 MIN 聚合函数表达式。
     *
     * @param column 要进行求最小值的列名
     * @return 包含 MIN 函数的 Expression 对象
     */
    public static Expression min(String column) {
        return new Expression("MIN(" + column + ")");
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
     * 创建一个 COUNT 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 COUNT 函数的 Expression 对象
     */
    public static <T> Expression count(TypeFunction<T, ?> fields) {
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("COUNT(" + column + ")");
    }

    /**
     * 创建一个 SUM 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 SUM 函数的 Expression 对象
     */
    public static <T> Expression sum(TypeFunction<T, ?> fields) {
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("SUM(" + column + ")");
    }

    /**
     * 创建一个 AVG 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 AVG 函数的 Expression 对象
     */
    public static <T> Expression avg(TypeFunction<T, ?> fields) {
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("AVG(" + column + ")");
    }

    /**
     * 创建一个 MAX 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 MAX 函数的 Expression 对象
     */
    public static <T> Expression max(TypeFunction<T, ?> fields) {
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("MAX(" + column + ")");
    }

    /**
     * 创建一个 MIN 聚合函数表达式（支持 Lambda 表达式）。
     *
     * @param fields 一个 TypeFunction 类型的 Lambda 表达式，用于引用实体类的属性
     * @param <T>    实体类的类型
     * @return 包含 MIN 函数的 Expression 对象
     */
    public static <T> Expression min(TypeFunction<T, ?> fields) {
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression("MIN(" + column + ")");
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
        String column = LambdaUtils.getLambdaColumnName(fields);
        return new Expression(column);
    }
}
