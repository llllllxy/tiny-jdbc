package org.tinycloud.jdbc.criteria;


import org.tinycloud.jdbc.exception.JdbcException;

import java.util.ArrayList;
import java.util.List;

/**
 * 条件构造器抽象类，抽象了一些公共的方法
 *
 * @author liuxingyu01
 * @since 2023-08-02
 **/
public abstract class AbstractCriteria {
    private static final String datetimePattern = "yyyy-MM-dd HH:mm:ss";

    private static final String timestampPattern = "yyyy-MM-dd HH:mm:ss:SSS";

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
    public AbstractCriteria() {
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
        return parameters;
    }

    /**
     * 根据条件生成对应的条件SQL
     * <pre>
     *  如： WHERE age < 28 AND name IN ('Bob', 'John') AND created_at = '2023-08-05 16:08:11' ORDER BY age DESC
     * <pre>
     * @return 条件SQL
     */
    public String generateSql() {
        StringBuilder sql = new StringBuilder();
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i == 0) {
                    if (conditions.get(i).startsWith(" OR ")) {
                        throw new JdbcException("Criteria can not start with a function orXXX!");
                    }
                    sql.append(conditions.get(i).replace(" AND ", ""));
                } else {
                    sql.append(conditions.get(i));
                }
            }
        }
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                if (i > 0) {
                    sql.append(",");
                }
                sql.append(orderBy.get(i));
            }
        }
        return sql.toString();
    }

    /**
     * 用于子构造器SQL的生成
     *
     * @return 条件SQL
     */
    public String children() {
        StringBuilder sql = new StringBuilder();
        if (!conditions.isEmpty()) {
            sql.append("(");
            for (int i = 0; i < conditions.size(); i++) {
                if (i == 0) {
                    if (conditions.get(i).startsWith(" OR ")) {
                        throw new JdbcException("Criteria can not start with a function orXXX!");
                    }
                    sql.append(conditions.get(i).replace(" AND ", ""));
                } else {
                    sql.append(conditions.get(i));
                }
            }
            sql.append(")");
        }
        return sql.toString();
    }
}
