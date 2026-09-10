package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.exception.TinyJdbcException;
import org.tinycloud.jdbc.util.LambdaUtils;
import org.tinycloud.jdbc.util.SqlIdentifierUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 条件构造器抽象类，抽象了一些公共的方法
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public abstract class Criteria<T> {

    /**
     * 标记下一个条件是否用 OR 连接（默认 AND）
     */
    protected boolean nextIsOr;

    /**
     * 查询字段-键
     */
    protected final List<String> selectFields;

    /**
     * 查询条件-键
     */
    protected final List<String> conditions;

    /**
     * 查询条件-值
     */
    protected final List<Object> whereParameters;

    /**
     * 更新字段和值（按插入顺序保存，重复字段会被覆盖）
     */
    protected final Map<String, Object> updateValues;

    /**
     * 排序的条件
     */
    protected final List<String> orderBys;

    /**
     * 排序的条件
     */
    protected final List<String> lastSqls;

    /**
     * 分组字段（GROUP BY）
     */
    protected final List<String> groupBys;

    /**
     * HAVING 条件（SQL 片段，含 ? 占位符）
     */
    protected final List<String> havings;

    /**
     * 构造方法
     */
    public Criteria() {
        this.nextIsOr = false;
        this.updateValues = new LinkedHashMap<>();
        this.selectFields = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.orderBys = new ArrayList<>();
        this.whereParameters = new ArrayList<>();
        this.lastSqls = new ArrayList<>();
        this.groupBys = new ArrayList<>();
        this.havings = new ArrayList<>();
    }

    /**
     * 获取条件前缀（根据 isNextOr 决定是 AND 还是 OR）
     * 执行后重置 isNextOr 为 false，避免影响后续条件
     */
    public String getConditionPrefix() {
        String prefix = this.nextIsOr ? " OR " : " AND ";
        // 重置状态，确保下一个条件默认用 AND
        this.nextIsOr = false;
        return prefix;
    }

    /**
     * 校验并原样返回一个合法的「列引用」。非法时抛出 {@link TinyJdbcException}，
     * 供各 String 形式的字段条件方法复用。
     *
     * @param columnRef 列引用
     * @return 校验通过的列引用
     */
    protected final String checkedColumnRef(String columnRef) {
        SqlIdentifierUtils.checkColumnRef(columnRef);
        return columnRef;
    }

    /**
     * 获取所有的参数值
     *
     * @return 参数列表
     */
    public List<Object> getParameters() {
        List<Object> parameters = new ArrayList<>();
        for (Object updateValue : this.updateValues.values()) {
            if (!(updateValue instanceof RawUpdateSqlValue)) {
                parameters.add(updateValue);
            }
        }
        parameters.addAll(this.whereParameters);
        return parameters;
    }

    /**
     * 获取查询条件参数（WHERE 与 HAVING），不包含更新 SET 参数。
     *
     * @return 条件参数副本
     */
    public List<Object> getConditionParameters() {
        return new ArrayList<>(this.whereParameters);
    }

    /**
     * 是否包含 GROUP BY 或 HAVING。
     *
     * @return true=统计时需要包裹分组子查询
     */
    public boolean hasGroupByOrHaving() {
        return !this.groupBys.isEmpty() || !this.havings.isEmpty();
    }

    /**
     * 根据条件生成对应查询部分的SQL片段
     *
     * @return 查询SQL片段
     */
    public String selectSql() {
        StringBuilder select = new StringBuilder();
        if (!this.selectFields.isEmpty()) {
            select.append(String.join(",", this.selectFields));
        }
        return select.toString();
    }

    /**
     * 根据条件生成对应更新部分的SQL片段
     *
     * <pre>
     * 如： id=?,create_time=?
     *
     * <pre>
     *
     * @return 更新SQL片段
     */
    public String updateSql() {
        StringBuilder update = new StringBuilder();
        if (!this.updateValues.isEmpty()) {
            int index = 0;
            for (Map.Entry<String, Object> entry : this.updateValues.entrySet()) {
                if (index > 0) {
                    update.append(",");
                }
                String column = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof RawUpdateSqlValue) {
                    update.append(column).append(" = ").append(((RawUpdateSqlValue) value).getSqlExpression());
                } else {
                    update.append(column).append(" = ?");
                }
                index++;
            }
        }
        return update.toString();
    }

    /**
     * 判断更新字段是否已存在
     *
     * @param columnName 数据库字段名
     * @return true=已存在，false=不存在
     */
    public boolean hasUpdateColumn(String columnName) {
        return this.updateValues.containsKey(columnName);
    }

    /**
     * 判断更新字段是否已存在
     *
     * @param field 字段引用
     * @return true=已存在，false=不存在
     */
    public boolean hasUpdateColumn(TypeFunction<T, ?> field) {
        String columnName = this.getColumnName(field);
        return this.updateValues.containsKey(columnName);
    }

    /**
     * Lambda获取列名
     */
    public String getColumnName(TypeFunction<T, ?> field) {
        return LambdaUtils.getLambdaColumnName(field);
    }

    /**
     * 根据条件生成对应的条件部分的SQL片段，带WHERE
     *
     * <pre>
     * 如： WHERE age < 28 AND name IN ('Bob', 'John') AND created_at = '2023-08-05
     * 16:08:11' ORDER BY age DESC
     *
     * <pre>
     *
     * @return 条件SQL片段
     */
    public String whereSql() {
        StringBuilder sql = new StringBuilder(whereConditions());
        sql.append(groupBySql());
        sql.append(havingSql());
        if (!this.orderBys.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(",", this.orderBys));
        }
        if (!this.lastSqls.isEmpty()) {
            sql.append(" ").append(this.lastSqls.get(0));
        }
        return sql.toString();
    }

    /**
     * 生成统计查询所需的条件 SQL：WHERE + GROUP BY + HAVING。
     * 不包含 ORDER BY 与 last() 尾片段。
     *
     * @return 统计条件 SQL 片段
     */
    public String whereGroupByHavingSql() {
        return this.whereConditions() + this.groupBySql() + this.havingSql();
    }

    /**
     * 生成 GROUP BY 片段（如 {@code " GROUP BY name,age"}）；无分组时返回空串。
     */
    public String groupBySql() {
        if (this.groupBys.isEmpty()) {
            return "";
        }
        return " GROUP BY " + String.join(",", this.groupBys);
    }

    /**
     * 生成 HAVING 片段（多个条件以 {@code AND} 连接）；无 HAVING 时返回空串。
     */
    public String havingSql() {
        if (this.havings.isEmpty()) {
            return "";
        }
        StringBuilder sql = new StringBuilder(" HAVING ");
        for (int i = 0; i < this.havings.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(this.havings.get(i));
        }
        return sql.toString();
    }

    /**
     * 追加一个 HAVING 条件及其参数。参数会被追加到条件参数列表，按「WHERE ... HAVING ...」顺序绑定。
     *
     * @param expression HAVING 表达式（含 {@code ?} 占位符，如 {@code "count(*) > ?"}）
     * @param params     绑定到 {@code ?} 的参数
     */
    protected final void addHaving(String expression, Object... params) {
        this.havings.add(expression);
        if (params != null) {
            Collections.addAll(this.whereParameters, params);
        }
    }

    /**
     * 仅生成条件部分的 SQL 片段（带 {@code WHERE}），<b>不包含</b> {@code ORDER BY} 与
     * {@code last()} 尾片段。
     *
     * <p>{@link #whereSql()} 会在此基础上追加排序与尾片段；而统计总数等场景
     * 只需条件本身（例如 {@code SELECT COUNT(*) ...}），不应携带排序或行锁等尾选项，
     * 否则在 PostgreSQL 等数据库上会产生非法或语义错误的聚合查询。</p>
     *
     * <pre>
     * 如： WHERE age < 28 AND name IN ('Bob', 'John')
     * </pre>
     *
     * @return 条件 SQL 片段；无条件时返回空串
     */
    public String whereConditions() {
        StringBuilder sql = new StringBuilder();
        if (!this.conditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < this.conditions.size(); i++) {
                String condition = this.conditions.get(i);
                if (i == 0) {
                    if (condition.startsWith(" OR ")) {
                        throw new TinyJdbcException("Criteria can not start with a function OR!");
                    }
                    if (condition.startsWith(" AND ")) {
                        condition = condition.substring(5);
                    }
                }
                sql.append(condition);
            }
        }
        return sql.toString();
    }

    /**
     * 用于构造子条件SQL片段的生成
     *
     * @return 子条件SQL片段
     */
    public String children() {
        StringBuilder sql = new StringBuilder();
        if (!this.conditions.isEmpty()) {
            sql.append("(");
            for (int i = 0; i < this.conditions.size(); i++) {
                String condition = this.conditions.get(i);
                if (i == 0) {
                    if (condition.startsWith(" OR ")) {
                        throw new TinyJdbcException("Criteria can not start with a function OR!");
                    }
                    if (condition.startsWith(" AND ")) {
                        condition = condition.substring(5);
                    }
                }
                sql.append(condition);
            }
            sql.append(")");
        }
        return sql.toString();
    }


    /**
     * 校验 IN/NOT IN 条件的参数集合不能为空。
     *
     * @param field  字段名
     * @param values 条件参数集合
     */
    protected void validateInValues(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            throw new TinyJdbcException("The values of IN/NOT IN condition cannot be null or empty, field: " + field);
        }
    }
}
