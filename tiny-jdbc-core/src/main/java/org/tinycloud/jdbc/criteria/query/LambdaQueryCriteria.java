package org.tinycloud.jdbc.criteria.query;

import org.tinycloud.jdbc.criteria.AbstractLambdaCriteria;
import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.ArrayUtils;

/**
 * <p>
 * 查询操作-条件构造器（Lambda版）
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class LambdaQueryCriteria<T> extends AbstractLambdaCriteria<T, LambdaQueryCriteria<T>> {

    /**
     * 指定查询的字段。
     *
     * @param field 可变参数，使用 TypeFunction 类型的 Lambda 表达式指定要查询的字段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    @SafeVarargs
    public final LambdaQueryCriteria<T> select(TypeFunction<T, ?>... field) {
        if (ArrayUtils.isNotEmpty(field)) {
            for (TypeFunction<T, ?> f : field) {
                String columnName = this.getColumnName(f);
                this.selectFields.add(columnName);
            }
        }
        return this;
    }

    /**
     * 根据指定字段进行排序。
     *
     * @param field  使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @param isDesc 是否为降序排序，true 表示降序，false 表示升序。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderBy(TypeFunction<T, ?> field, boolean isDesc) {
        return this.orderBy(true, field, isDesc);
    }

    /**
     * 根据指定条件和字段进行排序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @param isDesc  是否为降序排序，true 表示降序，false 表示升序。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderBy(boolean whether, TypeFunction<T, ?> field, boolean isDesc) {
        if (whether) {
            String columnName = this.getColumnName(field);
            if (isDesc) {
                columnName += " DESC";
            }
            this.orderBys.add(columnName);
        }
        return this;
    }

    /**
     * 根据指定字段进行升序排序。
     * 默认执行排序操作，排序方式为升序。
     *
     * @param field 使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderBy(TypeFunction<T, ?> field) {
        return this.orderBy(true, field, false);
    }

    /**
     * 根据指定条件和字段进行升序排序。
     * 默认排序方式为升序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderBy(boolean whether, TypeFunction<T, ?> field) {
        return this.orderBy(whether, field, false);
    }

    /**
     * 根据指定字段进行降序排序。
     * 默认执行排序操作，排序方式为降序。
     *
     * @param field 使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderByDesc(TypeFunction<T, ?> field) {
        return this.orderBy(true, field, true);
    }

    /**
     * 根据指定条件和字段进行降序排序。
     * 默认排序方式为降序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   使用 TypeFunction 类型的 Lambda 表达式指定排序字段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public LambdaQueryCriteria<T> orderByDesc(boolean whether, TypeFunction<T, ?> field) {
        return this.orderBy(whether, field, true);
    }


    /**
     * 在 SQL 语句末尾添加自定义 SQL 片段。
     * 会先清空之前添加的末尾 SQL 片段，再添加新的片段。
     *
     * @param lastSql 要添加到 SQL 语句末尾的自定义 SQL 片段。
     * @return 返回当前 LambdaQueryCriteria 对象，支持链式调用。
     */
    public final LambdaQueryCriteria<T> last(String lastSql) {
        this.lastSqls.clear();
        this.lastSqls.add(lastSql);
        return this;
    }

    /**
     * 创建并返回一个新的 LambdaQueryCriteria 实例。
     * 此方法为抽象方法的实现，用于在父类中创建新的实例。
     *
     * @return 新的 LambdaQueryCriteria 实例
     */
    @Override
    protected LambdaQueryCriteria<T> instance() {
        return new LambdaQueryCriteria<>();
    }
}
