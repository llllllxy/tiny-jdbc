package org.tinycloud.jdbc.criteria;


import org.tinycloud.jdbc.exception.TinyJdbcException;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件构造器抽象类，抽象了一些公共的方法
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public abstract class Criteria<T> {

    /**
     * 查询字段-键
     */
    protected final List<String> selectFields;

    /**
     * 修改字段-键
     */
    protected final List<String> updateFields;

    /**
     * 查询条件-键
     */
    protected final List<String> conditions;

    /**
     * 查询条件-值
     */
    protected final List<Object> parameters;

    /**
     * 排序的条件
     */
    protected final List<String> orderBy;

    /**
     * 构造方法
     */
    public Criteria() {
        this.updateFields = new ArrayList<>();
        this.selectFields = new ArrayList<>();
        this.conditions = new ArrayList<>();
        this.orderBy = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

    /**
     * 获取所有的参数值
     *
     * @return 参数列表
     */
    public List<Object> getParameters() {
        return this.parameters;
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
        if (!this.updateFields.isEmpty()) {
            update.append(String.join(",", this.updateFields));
        }
        return update.toString();
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
                if (i == 0) {
                    if (this.conditions.get(i).startsWith(" OR ")) {
                        throw new TinyJdbcException("Criteria can not start with a function orXXX!");
                    }
                    sql.append(this.conditions.get(i).replace(" AND ", ""));
                } else {
                    sql.append(this.conditions.get(i));
                }
            }
        }
        if (!this.orderBy.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(String.join(",", this.orderBy));
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
                if (i == 0) {
                    if (this.conditions.get(i).startsWith(" OR ")) {
                        throw new TinyJdbcException("Criteria can not start with a function orXXX!");
                    }
                    sql.append(this.conditions.get(i).replace(" AND ", ""));
                } else {
                    sql.append(this.conditions.get(i));
                }
            }
            sql.append(")");
        }
        return sql.toString();
    }
}
