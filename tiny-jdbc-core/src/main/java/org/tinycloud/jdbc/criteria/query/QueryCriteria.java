package org.tinycloud.jdbc.criteria.query;

import org.tinycloud.jdbc.criteria.AbstractCriteria;
import org.tinycloud.jdbc.sql.RawSql;
import org.tinycloud.jdbc.util.ArrayUtils;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

import java.util.Arrays;

/**
 * <p>
 * 查询操作-条件构造器
 * </p>
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public class QueryCriteria<T> extends AbstractCriteria<T, QueryCriteria<T>> {

    /**
     * 指定查询的字段。
     *
     * @param field 可变参数，要查询的字段名。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> select(String... field) {
        if (ArrayUtils.isNotEmpty(field)) {
            for (String f : field) {
                this.selectFields.add(this.checkedColumnRef(f));
            }
        }
        return this;
    }

    /**
     * 根据指定字段和排序方式进行排序。
     * 默认执行排序操作。
     *
     * @param field  排序字段名。
     * @param isDesc 是否为降序排序，true 表示降序，false 表示升序。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderBy(String field, boolean isDesc) {
        return this.orderBy(true, field, isDesc);
    }

    /**
     * 根据指定条件、字段和排序方式进行排序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   排序字段名。
     * @param isDesc  是否为降序排序，true 表示降序，false 表示升序。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderBy(boolean whether, String field, boolean isDesc) {
        if (whether) {
            String orderByString = this.checkedColumnRef(field);
            if (isDesc) {
                orderByString += " DESC";
            }
            this.orderBys.add(orderByString);
        }
        return this;
    }

    /**
     * 根据指定字段进行升序排序。
     * 默认执行排序操作，排序方式为升序。
     *
     * @param field 排序字段名。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderBy(String field) {
        return this.orderBy(true, field, false);
    }

    /**
     * 根据指定条件和字段进行升序排序。
     * 默认排序方式为升序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   排序字段名。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderBy(boolean whether, String field) {
        return this.orderBy(whether, field, false);
    }

    /**
     * 根据指定字段进行降序排序。
     * 默认执行排序操作，排序方式为降序。
     *
     * @param field 排序字段名。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderByDesc(String field) {
        return this.orderBy(true, field, true);
    }

    /**
     * 根据指定条件和字段进行降序排序。
     * 默认排序方式为降序。
     *
     * @param whether 是否执行排序操作，true 表示执行，false 表示不执行。
     * @param field   排序字段名。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> orderByDesc(boolean whether, String field) {
        return this.orderBy(whether, field, true);
    }

    /**
     * 在 SQL 语句末尾添加自定义 SQL 片段。
     * 会先清空之前添加的末尾 SQL 片段，再添加新的片段。
     * <p>
     * <b>安全说明：</b>{@code last()} 的入参会被当作原始 SQL 尾部片段追加，不会被参数化。
     * 默认会做一次「尾部片段安全校验」，拒绝包含分号 / 引号 / 注释 / {@code --} / {@code #} 等
     * 可能截断或拼接语句的内容。若确需追加不受限的原始 SQL，请用
     * {@link #last(RawSql)} 显式授权（只应传入可信常量）。
     * </p>
     *
     * @param lastSql 要添加到 SQL 语句末尾的自定义 SQL 片段。
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> last(String lastSql) {
        SqlIdentifierUtils.checkTailSql(lastSql);
        this.lastSqls.clear();
        this.lastSqls.add(lastSql);
        return this;
    }

    /**
     * 在 SQL 语句末尾添加一段<b>受信任</b>的原始 SQL 片段。
     * <p>
     * 用 {@link RawSql#wrap(String)} 显式包裹后即可跳过默认的尾部片段安全校验。
     * 请仅传入可信、常量内容。
     * </p>
     *
     * @param lastSql 受信任的 SQL 片段
     * @return 返回当前 QueryCriteria 对象，支持链式调用。
     */
    public final QueryCriteria<T> last(RawSql lastSql) {
        this.lastSqls.clear();
        this.lastSqls.add(lastSql.sql());
        return this;
    }

    /**
     * 创建并返回一个新的 QueryCriteria 实例。
     * 此方法为抽象方法的实现，用于在父类中创建新的实例。
     *
     * @return 新的 QueryCriteria 实例
     */
    @Override
    protected QueryCriteria<T> instance() {
        return new QueryCriteria<T>();
    }
}