package org.tinycloud.jdbc.criteria;

import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.ArrayList;
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
     * 根据条件生成对应查询部分的SQL片段
     * <pre>
     *  如： id,create_time
     * <pre>
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
     * <pre>
     *  如： id=?,create_time=?
     * <pre>
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
     * 根据条件生成对应的条件部分的SQL片段，带WHERE
     * <pre>
     *  如： WHERE age < 28 AND name IN ('Bob', 'John') AND created_at = '2023-08-05 16:08:11' ORDER BY age DESC
     * <pre>
     * @return 条件SQL片段
     */
    public String whereSql() {
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
        if (!this.orderBys.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(",", this.orderBys));
        }
        if (!this.lastSqls.isEmpty()) {
            sql.append(" ").append(this.lastSqls.get(0));
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
}
