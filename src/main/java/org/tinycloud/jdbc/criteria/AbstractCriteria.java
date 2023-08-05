package org.tinycloud.jdbc.criteria;

import org.springframework.format.datetime.DateFormatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
     * 查询条件
     */
    protected final List<String> conditions;

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
    }

    /**
     * 格式化sql参数值
     *
     * @param value 值
     * @return 格式化之后的值
     */
    protected String formatValue(Object value) {
        if (value instanceof String) {
            return "'" + value + "'";
        } else if (value instanceof java.util.Date) {
            DateFormatter dateFormatter = new DateFormatter(datetimePattern);
            String datetime = dateFormatter.print((java.util.Date) value, Locale.getDefault());
            return "'" + datetime + "'";
        } else if (value instanceof java.time.LocalDateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datetimePattern);
            String datetime = ((LocalDateTime) value).format(formatter);
            return "'" + datetime + "'";
        } else {
            return String.valueOf(value);
        }
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
                if (i > 0) {
                    sql.append(" AND ");
                }
                sql.append(conditions.get(i));
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
}
